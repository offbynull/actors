/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.rpc.transport.transports.udp;

import com.offbynull.peernetic.common.concurrent.actor.ActorQueue;
import com.offbynull.peernetic.common.concurrent.actor.ActorQueueWriter;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.common.concurrent.actor.Message.MessageResponder;
import com.offbynull.peernetic.common.concurrent.actor.PushQueue;
import com.offbynull.peernetic.common.concurrent.actor.SelectorActorQueueNotifier;
import com.offbynull.peernetic.common.concurrent.actor.helpers.TimeoutManager;
import com.offbynull.peernetic.common.concurrent.actor.helpers.TimeoutManager.TimeoutManagerResult;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transport.actormessages.commands.SetDestinationCommand;
import com.offbynull.peernetic.rpc.transport.actormessages.commands.SendRequestCommand;
import com.offbynull.peernetic.rpc.transport.actormessages.commands.SendResponseCommand;
import com.offbynull.peernetic.rpc.transport.actormessages.commands.DropResponseCommand;
import com.offbynull.peernetic.rpc.transport.actormessages.events.RequestArrivedEvent;
import com.offbynull.peernetic.rpc.transport.actormessages.events.ResponseArrivedEvent;
import com.offbynull.peernetic.rpc.transport.actormessages.events.ResponseErroredEvent;
import com.offbynull.peernetic.rpc.transport.actormessages.events.ResponseTimedOutEvent;
import com.offbynull.peernetic.rpc.transport.transports.udp.OutgoingPacketManager.OutgoingPacketManagerResult;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

/**
 * A UDP transport implementation.
 * @author Kasra Faghihi
 */
public final class UdpTransport extends Transport<InetSocketAddress> {
    
    private InetSocketAddress listenAddress;
    private Selector selector;
    private int selectionKey = SelectionKey.OP_READ;

    private int cacheSize;
    private long timeout;
    
    private DatagramChannel channel;
    private MessageIdGenerator idGenerator;
    private MessageIdCache idCache;
    
    private OutgoingPacketManager<InetSocketAddress> pendingPacketManager;
    private TimeoutManager<MessageId, MessageResponder> sendingRequestTimeoutManager;
    private TimeoutManager<Long, InetSocketAddress> processingResponseTimeoutManager;
    
    private ByteBuffer buffer;
    
    private long nextMessageKey;
    
    private ActorQueueWriter dstWriter;

    /**
     * Constructs a {@link UdpTransport} object.
     * @param listenAddress address to listen on
     * @param bufferSize buffer size
     * @param cacheSize number of packet ids to cache
     * @param timeout timeout duration
     * @param incomingFilter incoming filter
     * @param outgoingFilter outgoing filter
     * @throws IOException on error
     * @throws IllegalArgumentException if port is out of range, or if any of the other arguments are {@code <= 0};
     * @throws NullPointerException if any arguments are {@code null}
     */
    public UdpTransport(InetSocketAddress listenAddress, int bufferSize, int cacheSize, long timeout,
            IncomingFilter<InetSocketAddress> incomingFilter, OutgoingFilter<InetSocketAddress> outgoingFilter) throws IOException {
        super(incomingFilter, outgoingFilter, true);
        
        Validate.notNull(listenAddress);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, bufferSize);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, cacheSize);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);

        this.listenAddress = listenAddress;
        this.selector = Selector.open();
        
        this.cacheSize = cacheSize;
        this.timeout = timeout;
        
        this.buffer = ByteBuffer.allocate(bufferSize);
    }

    @Override
    protected ActorQueue createQueue() {
        return new ActorQueue(new SelectorActorQueueNotifier(selector));
    }

    @Override
    protected void onStart() throws Exception {
        idGenerator = new MessageIdGenerator();
        idCache = new MessageIdCache(cacheSize);
        
        pendingPacketManager = new OutgoingPacketManager<>(); 
        sendingRequestTimeoutManager = new TimeoutManager<>();
        processingResponseTimeoutManager = new TimeoutManager<>();

        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            channel.socket().bind(listenAddress);
        } catch (RuntimeException | IOException e) {
            IOUtils.closeQuietly(selector);
            IOUtils.closeQuietly(channel);
            throw e;
        }
    }
    
    @Override
    protected long onStep(long timestamp, Iterator<Message> iterator, PushQueue pushQueue) throws Exception {
        // process messages
        while (iterator.hasNext()) {
            Message msg = iterator.next();
            Object content = msg.getContent();
            
            if (content instanceof SetDestinationCommand) {
                SetDestinationCommand rc = (SetDestinationCommand) content;
                dstWriter = rc.getWriter();
            } else if (content instanceof SendRequestCommand) {
                SendRequestCommand<InetSocketAddress> src = (SendRequestCommand) content;
                MessageResponder key = msg.getResponder();
                if (key == null) {
                    continue;
                }
                
                OutgoingMessage<InetSocketAddress> packet = new OutgoingMessage<>(src.getTo(), src.getData());
                pendingPacketManager.addRequestPacket(timestamp + timeout, packet, key);
            } else if (content instanceof SendResponseCommand) {
                SendResponseCommand<InetSocketAddress> src = (SendResponseCommand) content;
                Object id = msg.getResponseToId();
                if (id == null || !(id instanceof Long)) {
                    continue;
                }
                
                InetSocketAddress sender = processingResponseTimeoutManager.remove((long) id);
                if (sender == null) {
                    continue;
                }
                
                OutgoingMessage<InetSocketAddress> responsePacket = new OutgoingMessage<>(sender, src.getData());
                pendingPacketManager.addResponsePacket(timestamp + timeout, responsePacket);
            } else if (content instanceof DropResponseCommand) {
                DropResponseCommand trc = (DropResponseCommand) content;
                Object id = msg.getResponseToId();
                if (id == null || !(id instanceof Long)) {
                    continue;
                }
                
                processingResponseTimeoutManager.remove((Long) id);
            } 
        }
        
        
        
        // process timeouts and get the maximum amount to time until this method should be called again
        long nextHitTime = 0L;
        
        OutgoingPacketManagerResult pmResult = pendingPacketManager.process(timestamp);
        TimeoutManagerResult<MessageId, MessageResponder> sendingReqTmResult = sendingRequestTimeoutManager.evaluate(timestamp);
        TimeoutManagerResult<Long, InetSocketAddress> processingRespTmResult =
                processingResponseTimeoutManager.evaluate(timestamp);
        
        Collection<MessageResponder> requestNotSentOut = pmResult.getMessageRespondersForFailures();
        for (MessageResponder key : requestNotSentOut) {
            pushQueue.queueResponseMessage(key, new ResponseErroredEvent());
        }
        nextHitTime = Math.max(nextHitTime, pmResult.getNextTimeoutTimestamp());
        
        for (MessageResponder key : sendingReqTmResult.getTimedout().values()) {
            pushQueue.queueResponseMessage(key, new ResponseTimedOutEvent());
        }
        nextHitTime = Math.max(nextHitTime, sendingReqTmResult.getDurationUntilNextTimeout());

          // No need to iterate over processingRespTmResult, we just won't process the response if/when Drop/SendResponseCommand comes in
        nextHitTime = Math.max(nextHitTime, processingRespTmResult.getDurationUntilNextTimeout());

        
        
        // go through selected keys
        IncomingFilter<InetSocketAddress> incomingFilter = getIncomingFilter();
        OutgoingFilter<InetSocketAddress> outgoingFilter = getOutgoingFilter();
        
        Iterator keys = selector.selectedKeys().iterator();
        while (keys.hasNext()) {
            SelectionKey key = (SelectionKey) keys.next();
            keys.remove();

            if (!key.isValid()) {
                continue;
            }

            if (key.isReadable()) { // incoming data available
                buffer.clear();

                InetSocketAddress from = (InetSocketAddress) channel.receive(buffer);
                buffer.flip();

                ByteBuffer tempBuffer = incomingFilter.filter(from, buffer);

                if (MessageMarker.isRequest(tempBuffer)) {
                    MessageMarker.skipOver(tempBuffer);

                    MessageId id = MessageId.extractPrependedId(tempBuffer);
                    MessageId.skipOver(tempBuffer);

                    if (!idCache.add(from, id, PacketType.REQUEST)) {
                        //throw new RuntimeException("Duplicate messageid encountered");
                        continue;
                    }

                    long msgKey = nextMessageKey;
                    nextMessageKey++;
                    
                    processingResponseTimeoutManager.put(msgKey, from, timestamp + timeout);
                    
                    RequestArrivedEvent<InetSocketAddress> msg = new RequestArrivedEvent<>(from, tempBuffer, timestamp);
                    pushQueue.queueRespondableMessage(dstWriter, msgKey, msg);
                } else if (MessageMarker.isResponse(tempBuffer)) {
                    MessageMarker.skipOver(tempBuffer);

                    MessageId id = MessageId.extractPrependedId(tempBuffer);
                    MessageId.skipOver(tempBuffer);

                    if (!idCache.add(from, id, PacketType.RESPONSE)) {
                        //throw new RuntimeException("Duplicate messageid encountered");
                        continue;
                    }

                    MessageResponder responseKey = sendingRequestTimeoutManager.remove(id);

                    if (responseKey == null) {
                        continue;
                    }

                    ResponseArrivedEvent<InetSocketAddress> msg = new ResponseArrivedEvent<>(from, tempBuffer, timestamp);
                    pushQueue.queueResponseMessage(responseKey, msg);
                }
            } else if (key.isWritable()) { // ready for outgoing data
                OutgoingMessage<InetSocketAddress> outgoingPacket = pendingPacketManager.getNextOutgoingPacket();
                if (outgoingPacket == null) {
                    continue;
                }

                switch (outgoingPacket.getType()) {
                    case REQUEST: {
                        MessageId id = idGenerator.generate();

                        buffer.clear();
                        
                        MessageMarker.writeRequestMarker(buffer);
                        id.writeId(buffer);
                        buffer.put(outgoingPacket.getData());
                        buffer.flip();

                        InetSocketAddress to = outgoingPacket.getAddress();
                        ByteBuffer tempBuffer = outgoingFilter.filter(to, buffer);
                        
                        channel.send(tempBuffer, to);
                        
                        sendingRequestTimeoutManager.put(id, outgoingPacket.getResponseKey(), timestamp + timeout);

                        break;
                    }
                    case RESPONSE: {
                        MessageResponder responseKey = outgoingPacket.getResponseKey();
                        Long id = responseKey.getId(Long.class);
                        
                        if (id == null) {
                            continue;
                        }

                        MessageIdInstance idInstance = processingResponseTimeoutManager.get(id);
                        if (idInstance == null) {
                            continue;
                        }

                        InetSocketAddress to = outgoingPacket.getAddress();
                        MessageId id = idInstance.getId();

                        buffer.clear();

                        MessageMarker.writeResponseMarker(buffer);
                        id.writeId(buffer);
                        buffer.put(outgoingPacket.getData());

                        buffer.flip();

                        ByteBuffer tempBuffer = outgoingFilter.filter(to, buffer);

                        channel.send(tempBuffer, to);

                        break;
                    }
                    default:
                        throw new IllegalStateException();
                }
            }
        }
        
        
        
        // set selection key based on if there's commands available -- this works because the only commands available are send req
        // and send resp
        int newSelectionKey = SelectionKey.OP_READ;
        if (iterator.hasNext()) {
            newSelectionKey |= SelectionKey.OP_WRITE;
        }

        if (newSelectionKey != selectionKey) {
            selectionKey = newSelectionKey;
            try {
                channel.register(selector, selectionKey);
            } catch (ClosedChannelException cce) {
                throw new RuntimeException(cce);
            }
        }
        
        return timestamp + nextHitTime;
    }

    @Override
    protected void onStop(PushQueue responseQueue) throws Exception {
        IOUtils.closeQuietly(selector);
        IOUtils.closeQuietly(channel);

        for (MessageResponder responseKey : sendingRequestTimeoutManager.flush().getTimedout().values()) {
            ResponseErroredEvent rtoEvent = new ResponseErroredEvent();
            responseQueue.queueResponseMessage(responseKey, rtoEvent);
        }
    }
}
