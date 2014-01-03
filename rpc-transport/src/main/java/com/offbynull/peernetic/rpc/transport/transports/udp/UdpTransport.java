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

import com.offbynull.peernetic.rpc.transport.common.IncomingMessageManager;
import com.offbynull.peernetic.rpc.transport.common.OutgoingMessageManager;
import com.offbynull.peernetic.common.concurrent.actor.ActorQueue;
import com.offbynull.peernetic.common.concurrent.actor.ActorQueueWriter;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.common.concurrent.actor.Message.MessageResponder;
import com.offbynull.peernetic.common.concurrent.actor.PushQueue;
import com.offbynull.peernetic.common.concurrent.actor.SelectorActorQueueNotifier;
import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transport.SendRequestCommand;
import com.offbynull.peernetic.rpc.transport.SendResponseCommand;
import com.offbynull.peernetic.rpc.transport.DropResponseCommand;
import com.offbynull.peernetic.rpc.transport.RequestArrivedEvent;
import com.offbynull.peernetic.rpc.transport.ResponseArrivedEvent;
import com.offbynull.peernetic.rpc.transport.ResponseErroredEvent;
import com.offbynull.peernetic.rpc.transport.common.IncomingMessageManager.IncomingPacketManagerResult;
import com.offbynull.peernetic.rpc.transport.common.IncomingMessageManager.IncomingRequest;
import com.offbynull.peernetic.rpc.transport.common.IncomingMessageManager.IncomingResponse;
import com.offbynull.peernetic.rpc.transport.common.IncomingMessageManager.IncomingRequestInfo;
import com.offbynull.peernetic.rpc.transport.common.MessageId;
import com.offbynull.peernetic.rpc.transport.common.OutgoingMessageManager.OutgoingPacketManagerResult;
import com.offbynull.peernetic.rpc.transport.common.OutgoingMessageManager.Packet;
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
    private int selectionKey;

    private long packetFlushTimeout;
    private long outgoingResponseTimeout;
    private long incomingResponseTimeout;
    
    private DatagramChannel channel;
    
    private OutgoingMessageManager<InetSocketAddress> outgoingPacketManager;
    private IncomingMessageManager<InetSocketAddress> incomingPacketManager;
    private long nextId;
    
    private ByteBuffer buffer;
    
    private int cacheSize;
    
    private ActorQueueWriter dstWriter;

    /**
     * Constructs a {@link UdpTransport} object.
     * @param listenAddress address to listen on
     * @param bufferSize buffer size
     * @param cacheSize number of packet ids to cache
     * @param packetFlushTimeout timeout duration for outgoing packets to get flushed in to the selector
     * @param outgoingResponseTimeout timeout duration for responses for outgoing requests to arrive
     * @param incomingResponseTimeout timeout duration for responses for incoming requests to be processed
     * @throws IOException on error
     * @throws IllegalArgumentException if port is out of range, or if any of the other arguments are {@code <= 0};
     * @throws NullPointerException if any arguments are {@code null}
     */
    public UdpTransport(InetSocketAddress listenAddress, int bufferSize, int cacheSize, long packetFlushTimeout,
            long outgoingResponseTimeout, long incomingResponseTimeout) throws IOException {
        super(true);
        
        Validate.notNull(listenAddress);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, bufferSize);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, cacheSize);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, packetFlushTimeout);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, outgoingResponseTimeout);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, incomingResponseTimeout);

        this.listenAddress = listenAddress;

        this.packetFlushTimeout = packetFlushTimeout;
        this.outgoingResponseTimeout = outgoingResponseTimeout;
        this.incomingResponseTimeout = incomingResponseTimeout;
        
        this.buffer = ByteBuffer.allocate(bufferSize);
        
        this.cacheSize = cacheSize;
    }

    @Override
    protected ActorQueue createQueue() {
        try {
            this.selector = Selector.open();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return new ActorQueue(new SelectorActorQueueNotifier(selector));
    }

    @Override
    protected void onStart() throws Exception {
        dstWriter = getDestinationWriter();
        Validate.validState(dstWriter != null);
        
        outgoingPacketManager = new OutgoingMessageManager<>(getOutgoingFilter()); 
        incomingPacketManager = new IncomingMessageManager<>(cacheSize, getIncomingFilter());

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
            
            if (content instanceof SendRequestCommand) {
                SendRequestCommand<InetSocketAddress> src = (SendRequestCommand) content;
                MessageResponder responder = msg.getResponder();
                if (responder == null) {
                    continue;
                }
                
                long id = nextId++;
                outgoingPacketManager.outgoingRequest(id, src.getTo(), src.getData(), timestamp + packetFlushTimeout,
                        timestamp + outgoingResponseTimeout, responder);
            } else if (content instanceof SendResponseCommand) {
                SendResponseCommand<InetSocketAddress> src = (SendResponseCommand) content;
                Long id = msg.getResponseToId(Long.class);
                if (id == null) {
                    continue;
                }
                
                IncomingRequestInfo<InetSocketAddress> pendingRequest = incomingPacketManager.responseFormed(id);
                if (pendingRequest == null) {
                    continue;
                }
                
                outgoingPacketManager.outgoingResponse(id, pendingRequest.getFrom(), src.getData(), pendingRequest.getMessageId(),
                        timestamp + packetFlushTimeout);
            } else if (content instanceof DropResponseCommand) {
                //DropResponseCommand trc = (DropResponseCommand) content;
                Long id = msg.getResponseToId(Long.class);
                if (id == null) {
                    continue;
                }
                
                incomingPacketManager.responseFormed(id);
            } 
        }
        
        
        
        // process timeouts for outgoing requests
        OutgoingPacketManagerResult opmResult = outgoingPacketManager.process(timestamp);
        
        Collection<MessageResponder> respondersForFailures = opmResult.getMessageRespondersForFailures();
        for (MessageResponder responder : respondersForFailures) {
            responder.respondDeferred(pushQueue, new ResponseErroredEvent());
        }

        
        
        // go through selected keys
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
                
                long id = nextId++;
                incomingPacketManager.incomingData(id, from, buffer, timestamp + incomingResponseTimeout);
            } else if (key.isWritable()) { // ready for outgoing data
                Packet<InetSocketAddress> outgoingPacket = outgoingPacketManager.getNextOutgoingPacket();
                if (outgoingPacket == null) {
                    continue;
                }

                channel.send(outgoingPacket.getData(), outgoingPacket.getTo());
            }
        }
        
        
        
        // process timeouts for incoming requests
        IncomingPacketManagerResult<InetSocketAddress> ipmResult = incomingPacketManager.process(timestamp);
        
        for (IncomingRequest<InetSocketAddress> incomingRequest : ipmResult.getNewIncomingRequests()) {
            RequestArrivedEvent<InetSocketAddress> event = new RequestArrivedEvent<>(
                    incomingRequest.getFrom(),
                    incomingRequest.getData(),
                    timestamp);
            pushQueue.queueRespondableMessage(dstWriter, incomingRequest.getId(), event);
        }
        
        for (IncomingResponse<InetSocketAddress> incomingResponse : ipmResult.getNewIncomingResponses()) {
            MessageId messageId = incomingResponse.getMessageId();
            MessageResponder responder = outgoingPacketManager.responseReturned(messageId);
            
            if (responder == null) {
                continue;
            }
            
            ResponseArrivedEvent<InetSocketAddress> event = new ResponseArrivedEvent<>(
                    incomingResponse.getFrom(),
                    incomingResponse.getData(),
                    timestamp);
            responder.respondDeferred(pushQueue, event);
        }
        
        
        // set selection key based on if there's messages to go out
        int newSelectionKey = SelectionKey.OP_READ;
        if (opmResult.getPacketsAvailable() > 0) {
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
        
        
        // calculate max time until next invoke
        long waitTime = Long.MAX_VALUE;
        waitTime = Math.min(waitTime, opmResult.getMaxTimestamp());
        waitTime = Math.min(waitTime, ipmResult.getMaxTimestamp());
        
        return waitTime;
    }

    @Override
    protected void onStop(PushQueue pushQueue) throws Exception {
        IOUtils.closeQuietly(selector);
        IOUtils.closeQuietly(channel);

        for (MessageResponder responder : outgoingPacketManager.process(Long.MAX_VALUE).getMessageRespondersForFailures()) {
            ResponseErroredEvent ree = new ResponseErroredEvent();
            responder.respondDeferred(pushQueue, ree);
        }
    }
}
