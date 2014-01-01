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

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.offbynull.peernetic.common.concurrent.actor.ActorQueue;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.common.concurrent.actor.ResponseQueue;
import com.offbynull.peernetic.common.concurrent.actor.SelectorActorQueueNotifier;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transport.transports.udp.TimeoutManager.Result;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

/**
 * A UDP transport implementation.
 * @author Kasra Faghihi
 */
public final class UdpTransport extends Transport<InetSocketAddress> {
    
    private InetSocketAddress listenAddress;
    private Selector selector;
    
    private int bufferSize;
    private int cacheSize;
    private long timeout;
    
    private DatagramChannel channel;
    private MessageIdGenerator idGenerator;
    private MessageIdCache idCache;
    private TimeoutManager timeoutManager;

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
        
        this.bufferSize = bufferSize;
        this.cacheSize = cacheSize;
        this.timeout = timeout;
    }

    @Override
    protected ActorQueue createQueue() {
        return new ActorQueue(new SelectorActorQueueNotifier(selector));
    }

    @Override
    protected void onStart() throws Exception {
        idGenerator = new MessageIdGenerator();
        idCache = new MessageIdCache(cacheSize);
        timeoutManager = new TimeoutManager(timeout);

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
    protected long onStep(long timestamp, Iterator<Message> messages, ResponseQueue responseQueue) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void onStop() throws Exception {
        IOUtils.closeQuietly(selector);
        IOUtils.closeQuietly(channel);

        for (OutgoingMessageResponseListener receiver : timeoutManager.pending().getTimedOut()) {
            receiver.internalErrorOccurred(null);
        }
    }

    
    private final class EventLoop extends AbstractExecutionThreadService {



        

        @Override
        protected void run() {
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

            int selectionKey = SelectionKey.OP_READ;
            LinkedList<Event> internalEventQueue = new LinkedList<>();
            LinkedList<Command> dumpedCommandQueue = new LinkedList<>();
            while (true) {
                // get current time
                long currentTime = System.currentTimeMillis();
                
                // get outgoing data
                commandQueue.drainTo(dumpedCommandQueue);
                
                // set selection key based on if there's commands available -- this works because the only commands available are send req
                // and send resp
                int newSelectionKey = SelectionKey.OP_READ;
                if (!dumpedCommandQueue.isEmpty()) {
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
                
                // get timed out channels + max amount of time to wait till next timeout
                Result timeoutRes = timeoutManager.evaluate(currentTime);
                long waitDuration = timeoutRes.getWaitDuration();

                // go through receivers requestManager has removed for timing out and add timeout events for each of them
                boolean timeoutEventAdded = false;
                for (OutgoingMessageResponseListener<InetSocketAddress> receiver : timeoutRes.getTimedOut()) {
                    internalEventQueue.add(new EventResponseTimedOut(receiver));
                    
                    timeoutEventAdded = true;
                }
                
                // select
                try {
                    // if timeout event was added then don't wait, because we need to process those events
                    if (timeoutEventAdded) {
                        selector.selectNow();
                    } else {
                        selector.select(waitDuration);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


                
                // stop if signalled
                if (stop.get()) {
                    return;
                }
                
                // update current time
                currentTime = System.currentTimeMillis();
                
                // go through selected keys
                Iterator keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = (SelectionKey) keys.next();
                    keys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isReadable()) { // incoming data available
                        try {
                            buffer.clear();
                            
                            InetSocketAddress from = (InetSocketAddress) channel.receive(buffer);
                            buffer.flip();

                            ByteBuffer tempBuffer = inFilter.filter(from, buffer);
                            
                            if (MessageMarker.isRequest(tempBuffer)) {
                                MessageMarker.skipOver(tempBuffer);

                                MessageId id = MessageId.extractPrependedId(tempBuffer);
                                MessageId.skipOver(tempBuffer);
                                
                                if (!idCache.add(from, id, PacketType.REQUEST)) {
                                    throw new RuntimeException("Duplicate messageid encountered");
                                }

                                IncomingMessage<InetSocketAddress> request = new IncomingMessage<>(from, tempBuffer, currentTime);

                                EventRequestArrived eventRa = new EventRequestArrived(request, selector, id);
                                internalEventQueue.add(eventRa);
                            } else if (MessageMarker.isResponse(tempBuffer)) {
                                MessageMarker.skipOver(tempBuffer);

                                MessageId id = MessageId.extractPrependedId(tempBuffer);
                                MessageId.skipOver(tempBuffer);
                                
                                if (!idCache.add(from, id, PacketType.RESPONSE)) {
                                    throw new RuntimeException("Duplicate messageid encountered");
                                }

                                OutgoingMessageResponseListener<InetSocketAddress> receiver = timeoutManager.getResponseDetails(from, id);

                                if (receiver != null) { // timeout may have lapsed already, don't do anything if it did
                                    IncomingResponse<InetSocketAddress> response = new IncomingResponse<>(from, tempBuffer, currentTime);

                                    EventResponseArrived eventRa = new EventResponseArrived(response, receiver);
                                    internalEventQueue.add(eventRa);
                                }
                            } else {
                                throw new IllegalStateException();
                            }
                        } catch (RuntimeException | IOException e) { // NOPMD
                            // do nothing
                        }
                    } else if (key.isWritable()) { // ready for outgoing data
                        try {
                            Command command = dumpedCommandQueue.poll();

                            if (command instanceof CommandSendRequest) {
                                CommandSendRequest commandSr = (CommandSendRequest) command;

                                OutgoingMessage<InetSocketAddress> request = commandSr.getMessage();
                                OutgoingMessageResponseListener<InetSocketAddress> receiver = commandSr.getResponseListener();

                                MessageId id = idGenerator.generate();

                                buffer.clear();

                                MessageMarker.writeRequestMarker(buffer);
                                id.writeId(buffer);
                                buffer.put(request.getData());
                                
                                buffer.flip();

                                InetSocketAddress to = request.getTo();

                                timeoutManager.addRequestId(to, id, receiver, currentTime);
                                
                                ByteBuffer tempBuffer = outFilter.filter(to, buffer);
                                
                                channel.send(tempBuffer, to);
                            } else if (command instanceof CommandSendResponse) {
                                CommandSendResponse commandSr = (CommandSendResponse) command;

                                OutgoingResponse response = commandSr.getResponse();
                                InetSocketAddress to = commandSr.getAddress();
                                MessageId id = commandSr.getMessageId();

                                buffer.clear();

                                MessageMarker.writeResponseMarker(buffer);
                                id.writeId(buffer);
                                buffer.put(response.getData());
                                
                                buffer.flip();
                                
                                ByteBuffer tempBuffer = outFilter.filter(to, buffer);
                                
                                channel.send(tempBuffer, to);                        
                            } else {
                                throw new IllegalStateException();
                            }
                        } catch (RuntimeException | IOException e) { // NOPMD
                            // do nothing
                        }
                    }
                }
                processEvents(internalEventQueue);
                internalEventQueue.clear();
            }
        }

        private void processEvents(LinkedList<Event> internalEventQueue) {
            for (Event event : internalEventQueue) {
                if (event instanceof EventRequestArrived) {
                    EventRequestArrived request = (EventRequestArrived) event;
                    
                    IncomingMessage<InetSocketAddress> data = request.getRequest();
                    Selector selector = request.getSelector();
                    MessageId id = request.getId();
                    UdpIncomingMessageResponseHandler responseSender = new UdpIncomingMessageResponseHandler(commandQueue, selector, id,
                            data.getFrom());

                    try {
                        handler.messageArrived(data, responseSender);
                    } catch (RuntimeException re) {
                        // don't bother notifying the others
                        break;
                    }
                } else if (event instanceof EventResponseArrived) {
                    EventResponseArrived response = (EventResponseArrived) event;
                    IncomingResponse<InetSocketAddress> data = response.getResponse();

                    try {
                        response.getReceiver().responseArrived(data);
                    } catch (RuntimeException re) { // NOPMD
                        // do nothing
                    }
                } else if (event instanceof EventResponseTimedOut) {
                    EventResponseTimedOut response = (EventResponseTimedOut) event;

                    try {
                        response.getReceiver().timedOut();
                    } catch (RuntimeException re) { // NOPMD
                        // do nothing
                    }
                }
            }
        }
        

        @Override
        protected String serviceName() {
            return UdpTransport.class.getSimpleName() + " Event Loop (" + listenAddress + ")";
        }

        @Override
        protected void triggerShutdown() {
            stop.set(true);
            selector.wakeup();
        }
    }
}
