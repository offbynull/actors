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
package com.offbynull.rpc.transport.fake;

import com.offbynull.rpc.transport.IncomingFilter;
import com.offbynull.rpc.transport.IncomingMessage;
import com.offbynull.rpc.transport.IncomingMessageListener;
import com.offbynull.rpc.transport.IncomingMessageResponseHandler;
import com.offbynull.rpc.transport.IncomingResponse;
import com.offbynull.rpc.transport.OutgoingFilter;
import com.offbynull.rpc.transport.OutgoingMessage;
import com.offbynull.rpc.transport.OutgoingMessageResponseListener;
import com.offbynull.rpc.transport.OutgoingResponse;
import com.offbynull.rpc.transport.Transport;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

/**
 * A {@link Transport} used for testing. Backed by a {@link FakeHub}.
 * @author Kasra F
 * @param <A> address type
 */
public final class FakeTransport<A> implements Transport<A> {

    private static final byte SEND_MARKER = 0;
    private static final byte REPLY_MARKER = 1;
    
    private Lock lock;
    private IncomingMessageListener<A> listener;
    private IncomingFilter<A> inFilter;
    private OutgoingFilter<A> outFilter;
    private State state = State.UNKNOWN;
    private int nextPacketId;
    private A address;
    private FakeHub<A> hub;
    private FakeHubSender<A> hubSender;
    private FakeHubReceiver<A> hubReceiver;
    private Timer timeoutTimer;
    private Map<Integer, PendingResponse> responseIdMap;
    private long timeout;

    /**
     * Constructs a {@link FakeTransport} object.
     * @param address this object's address
     * @param hub hub to connect to
     * @param timeout timeout
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code timeout < 1L} 
     */
    public FakeTransport(A address, FakeHub<A> hub, long timeout) {
        Validate.notNull(address);
        Validate.notNull(hub);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);
        
        this.lock = new ReentrantLock();
        
        this.hub = hub;
        this.address = address;

        hubReceiver = new CustomFakeHubReceiver();
        hubSender = hub.addEndpoint(address, hubReceiver);

        responseIdMap = new HashMap<>();
        
        this.timeout = timeout;
    }

    @Override
    public void start(IncomingFilter<A> incomingFilter, IncomingMessageListener<A> incomingMessageListener,
            OutgoingFilter<A> outgoingFilter) throws IOException {
        lock.lock();
        try {
            Validate.validState(state == State.UNKNOWN);

            Validate.notNull(incomingFilter);
            Validate.notNull(outgoingFilter);
            Validate.notNull(incomingMessageListener);
            
            timeoutTimer = new Timer();
            
            this.listener = incomingMessageListener;
            this.inFilter = incomingFilter;
            this.outFilter = outgoingFilter;
            hub.activateEndpoint(address);

            state = State.STARTED;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void stop() throws IOException {
        lock.lock();
        try {
            Validate.validState(state == State.STARTED);
            
            timeoutTimer.cancel();
            
            state = State.STOPPED;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void sendMessage(OutgoingMessage<A> message, final OutgoingMessageResponseListener<A> listener) {
        Validate.notNull(message);
        Validate.notNull(listener);

        lock.lock();
        try {
            Validate.validState(state == State.STARTED);
            final int packetId = nextPacketId++;
            
            A to = message.getTo();

            ByteBuffer buffer = message.getData();
            ByteBuffer newBuffer = ByteBuffer.allocate(5 + buffer.remaining());

            newBuffer.put(SEND_MARKER);
            newBuffer.putInt(packetId);
            newBuffer.put(buffer);
            newBuffer.position(0);
            
            ByteBuffer tempBuffer = outFilter.filter(to, newBuffer);

            hubSender.send(address, to, tempBuffer);
            
            TimerTask timerTask = new TimerTask() {

                @Override
                public void run() {
                    lock.lock();
                    try {
                        PendingResponse pr = responseIdMap.remove(packetId);
                        if (pr == null) {
                            return;
                        }
                        
                        pr.getListener().timedOut();
                    } finally {
                        lock.unlock();
                    }
                }
            };
            
            responseIdMap.put(packetId, new PendingResponse(to, packetId, timerTask, listener));
            timeoutTimer.schedule(timerTask, timeout);
        } finally {
            lock.unlock();
        }
    }

    private enum State {

        UNKNOWN,
        STARTED,
        STOPPED
    }

    private final class CustomFakeHubReceiver implements FakeHubReceiver<A> {

        @Override
        public void incoming(Message<A> packet) {
            A from = packet.getFrom();
            ByteBuffer data = packet.getData();
            byte marker = data.get();
            int packetId = data.getInt();
            
            lock.lock();
            try {
                Validate.validState(state == State.STARTED);
                
                ByteBuffer tempBuffer = outFilter.filter(from, data);
                
                switch (marker) {
                    case SEND_MARKER: {
                        IncomingMessage<A> message = new IncomingMessage<>(from, tempBuffer, System.currentTimeMillis());
                        IncomingMessageResponseHandler handler = new CustomIncomingMessageResponseHandler(from, packetId);
                        listener.messageArrived(message, handler);
                        break;
                    }
                    case REPLY_MARKER: {
                        PendingResponse pr = responseIdMap.get(packetId);
                        
                        if (pr == null || !pr.getAddress().equals(from)) {
                            return;
                        }
                        
                        OutgoingMessageResponseListener<A> listener = pr.getListener();
                        IncomingResponse<A> response = new IncomingResponse(from, tempBuffer, System.currentTimeMillis());
                        
                        listener.responseArrived(response);
                        break;
                    }
                    default:
                        throw new IllegalArgumentException();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private final class CustomIncomingMessageResponseHandler implements IncomingMessageResponseHandler {

        private A replyTo;
        private int packetId;

        public CustomIncomingMessageResponseHandler(A replyTo, int packetId) {
            this.replyTo = replyTo;
            this.packetId = packetId;
        }

        @Override
        public void responseReady(OutgoingResponse response) {
            lock.lock();
            try {
                Validate.validState(state == State.STARTED);
                
                ByteBuffer buffer = response.getData();
                ByteBuffer newBuffer = ByteBuffer.allocate(5 + buffer.remaining());

                newBuffer.put(REPLY_MARKER);
                newBuffer.putInt(packetId);
                newBuffer.put(buffer);
                newBuffer.flip();
            
                hubSender.send(address, replyTo, newBuffer);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void terminate() {
            // do nothing
        }

    }
    
    private final class PendingResponse {
        private A address;
        private int packetId;
        private TimerTask timerTask;
        private OutgoingMessageResponseListener<A> listener;

        public PendingResponse(A address, int packetId, TimerTask timerTask, OutgoingMessageResponseListener<A> listener) {
            this.address = address;
            this.packetId = packetId;
            this.timerTask = timerTask;
            this.listener = listener;
        }

        public A getAddress() {
            return address;
        }

        public int getPacketId() {
            return packetId;
        }

        public TimerTask getTimerTask() {
            return timerTask;
        }

        public OutgoingMessageResponseListener<A> getListener() {
            return listener;
        }
    }
}
