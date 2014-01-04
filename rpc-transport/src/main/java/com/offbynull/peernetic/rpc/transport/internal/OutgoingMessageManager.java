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
package com.offbynull.peernetic.rpc.transport.internal;

import com.offbynull.peernetic.common.concurrent.actor.helpers.TimeoutManager;
import com.offbynull.peernetic.common.concurrent.actor.helpers.TimeoutManager.TimeoutManagerResult;
import com.offbynull.peernetic.common.nio.utils.ByteBufferUtils;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import com.offbynull.peernetic.rpc.transport.OutgoingMessageResponseListener;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;

/**
 * Manages outgoing messages.
 * <p/>
 * New outgoing requests should be piped in to {@link #outgoingRequest(long, java.lang.Object, java.nio.ByteBuffer, long, long,
 * com.offbynull.peernetic.common.concurrent.actor.Message.MessageResponder) }. New outgoing responses should be piped in to
 * {@link #outgoingResponse(long, java.lang.Object, java.nio.ByteBuffer, com.offbynull.peernetic.rpc.transport.common.MessageId, long) }.
 * Requests will be tracked internally until a response for the request arrives
 * ({@link #responseReturned(com.offbynull.peernetic.rpc.transport.common.MessageId) }) or until a timeout is reached. Responses won't.
 * Requests / responses that have been pushed in will be returned one-by-one as {@link #getNextOutgoingPacket() } is called.
 * Timed out requests will be returned by {@link #process(long) }. 
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class OutgoingMessageManager<A> {
    private OutgoingFilter<A> outgoingFilter;
    
    private MessageIdGenerator idGenerator = new MessageIdGenerator();
    
    private TimeoutManager<Long> queuedRequestTimeoutManager = new TimeoutManager<>();
    private TimeoutManager<Long> sentRequestTimeoutManager = new TimeoutManager<>();
    private Map<Long, OutgoingRequest> requestIdLookup = new HashMap<>();
    private Map<MessageId, Long> requestMessageIdLookup = new HashMap<>();
    private TimeoutManager<Long> queuedResponseTimeoutManager = new TimeoutManager<>();
    private LinkedList<SendEntity> queuedSends = new LinkedList<>();

    /**
     * Constructs a {@link OutgoingMessageManager} object.
     * @param outgoingFilter outgoing filter
     * @throws NullPointerException if any arguments are {@code null}
     */
    public OutgoingMessageManager(OutgoingFilter<A> outgoingFilter) {
        Validate.notNull(outgoingFilter);
        
        this.outgoingFilter = outgoingFilter;
    }
    
    /**
     * Called when a request is ready to go out. Request are tracked internally until either a response comes in for the request
     * ({@link #responseReturned(com.offbynull.peernetic.rpc.transport.common.MessageId) }) or a timeout is reached.
     * @param id internal id
     * @param to destination address
     * @param data request data
     * @param queueTimestampTimeout maximum amount of time this request can take to go on the network before being discarded
     * @param sentTimestampTimeout maximum amount of time to wait for a response for this request
     * @param listener listener to issue a response/error to
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void outgoingRequest(long id, A to, ByteBuffer data, long queueTimestampTimeout, long sentTimestampTimeout,
            OutgoingMessageResponseListener listener) {
        Validate.notNull(to);
        Validate.notNull(data);
        Validate.notNull(listener);

        MessageId messageId = idGenerator.generate();

        ByteBuffer tempBuffer = ByteBuffer.allocate(data.remaining() + MessageMarker.MARKER_SIZE + 32);

        MessageMarker.writeMarker(tempBuffer, MessageType.REQUEST);
        messageId.writeId(tempBuffer);
        tempBuffer.put(data);
        tempBuffer.flip();

        ByteBuffer localTempBuffer = outgoingFilter.filter(to, tempBuffer);
        
        if (localTempBuffer == tempBuffer) {
            localTempBuffer = ByteBufferUtils.copyContents(tempBuffer);
        }
        
        OutgoingRequest request = new OutgoingRequest(id, messageId, localTempBuffer, listener, to);
        
        if (requestMessageIdLookup.containsKey(messageId)) {
            // Duplicate hit. This should never happen. If it does, just ignore this packet
            return;
        }
        
        queuedSends.add(request);
        queuedRequestTimeoutManager.add(id, queueTimestampTimeout);
        sentRequestTimeoutManager.add(id, sentTimestampTimeout);
        requestIdLookup.put(id, request);
        requestMessageIdLookup.put(messageId, id);
    }

    /**
     * Called when a response is ready to go out.
     * @param id internal id
     * @param to destination address
     * @param data response data
     * @param messageId message id this response is for
     * @param queueTimestampTimeout maximum amount of time this request can take to go on the network before being discarded
     */
    public void outgoingResponse(long id, A to, ByteBuffer data, MessageId messageId, long queueTimestampTimeout) {
        Validate.notNull(to);
        Validate.notNull(data);
        Validate.notNull(messageId);
        
        ByteBuffer tempBuffer = ByteBuffer.allocate(data.remaining() + 1 + 32);

        MessageMarker.writeMarker(tempBuffer, MessageType.RESPONSE);
        messageId.writeId(tempBuffer);
        tempBuffer.put(data);
        tempBuffer.flip();

        ByteBuffer localTempBuffer = outgoingFilter.filter(to, tempBuffer);
        
        if (localTempBuffer == tempBuffer) {
            localTempBuffer = ByteBufferUtils.copyContents(tempBuffer);
        }

        OutgoingResponse response = new OutgoingResponse(id, localTempBuffer, to);
        
        
        queuedSends.add(response);
    }

    /**
     * Get the next packet in the outgoing queue.
     * @return next packet in the outgoing queue, or {@code null} if no more packets are available
     */
    public Packet<A> getNextOutgoingPacket() {
        SendEntity sendEntity = null;
        while (true) {
            sendEntity = queuedSends.poll();
            
            if (sendEntity == null) {
                return null;
            }
            
            if (!sendEntity.isCancelled()) {
                break;
            }
        }
        
        Packet<A> packet;
        if (sendEntity instanceof OutgoingMessageManager.OutgoingRequest) {
            OutgoingRequest<A> req = (OutgoingRequest) sendEntity;
            queuedRequestTimeoutManager.cancel(req.getId());
            packet = new Packet<>(req.getData(), req.getTo());
        } else if (sendEntity instanceof OutgoingMessageManager.OutgoingResponse) {
            OutgoingResponse<A> resp = (OutgoingResponse<A>) sendEntity;
            packet = new Packet<>(resp.getData(), resp.getDestination());
        } else {
            throw new IllegalStateException();
        }
        
        return packet;
    }

    /**
     * Called once per step. Returns the request packets that have timed out (both from waiting too long to be flushed to the network and
     * from waiting too long for a response to arrive) as well as the next timestamp this method should be called.
     * again.
     * @param timestamp current timestamp
     * @return new request/responses, timed out requests, and the next time this method should be called again
     */
    public OutgoingMessageManagerResult process(long timestamp) {
        Set<OutgoingMessageResponseListener> listenersForFailures = new HashSet<>();
        long maxTimestamp = Long.MAX_VALUE;
        
        TimeoutManagerResult<Long> timedOutQueuedRequests =  queuedRequestTimeoutManager.process(timestamp);
        for (Long id : timedOutQueuedRequests.getTimedout()) {
            sentRequestTimeoutManager.cancel(id);
            OutgoingRequest request = requestIdLookup.remove(id);
            requestMessageIdLookup.remove(request.getMessageId());
            listenersForFailures.add(request.getListener());
            request.cancel();
        }
        maxTimestamp = Math.min(maxTimestamp, timedOutQueuedRequests.getNextTimeoutTimestamp());
        
        TimeoutManagerResult<Long> timedOutSentRequests = sentRequestTimeoutManager.process(timestamp);
        for (Long id : timedOutSentRequests.getTimedout()) {
            OutgoingRequest request = requestIdLookup.remove(id);
            requestMessageIdLookup.remove(request.getMessageId());
            listenersForFailures.add(request.getListener());
            request.cancel();
        }
        maxTimestamp = Math.min(maxTimestamp, timedOutSentRequests.getNextTimeoutTimestamp());
        
        TimeoutManagerResult<Long> timedOutQueuedResponses = queuedResponseTimeoutManager.process(timestamp);
        maxTimestamp = Math.min(maxTimestamp, timedOutQueuedResponses.getNextTimeoutTimestamp());
        
        return new OutgoingMessageManagerResult(listenersForFailures, maxTimestamp, queuedSends.size());
    }

    /**
     * Called when a response comes in.
     * @param messageId message id of the request this response is for
     * @return listener for the request that the response is for, or {@code null} if the internal reference to the request no longer exists
     * (because it timed out or never existed in the first place)
     */
    public OutgoingMessageResponseListener responseReturned(MessageId messageId) {
        Validate.notNull(messageId);
        
        Long internalId = requestMessageIdLookup.remove(messageId);
        if (internalId == null) {
            return null;
        }
        
        sentRequestTimeoutManager.cancel(internalId);
        OutgoingRequest request = requestIdLookup.remove(internalId);
        
        if (request == null) {
            return null;
        }
        
        return request.getListener();
    }

    /**
     * An outgoing packet.
     * @param <A> address type
     */
    public static final class Packet<A> {
        private ByteBuffer data;
        private A to;

        private Packet(ByteBuffer data, A to) {
            Validate.notNull(data);
            Validate.notNull(to);
            
            this.data = data;
            this.to = to;
        }

        /**
         * Get data.
         * @return data
         */
        public ByteBuffer getData() {
            return data;
        }

        /**
         * Get destination address.
         * @return destination address
         */
        public A getTo() {
            return to;
        }
        
    }

    private static class SendEntity {
        private boolean cancelled;

        public boolean isCancelled() {
            return cancelled;
        }

        public void cancel() {
            this.cancelled = true;
        }
        
    }
    
    private static final class OutgoingRequest<A> extends SendEntity {
        private Long id;
        private MessageId messageId;
        private ByteBuffer data;
        private OutgoingMessageResponseListener listener;
        private A to;

        public OutgoingRequest(Long id, MessageId messageId, ByteBuffer data, OutgoingMessageResponseListener listener, A to) {
            Validate.notNull(id);
            Validate.notNull(messageId);
            Validate.notNull(data);
            Validate.notNull(listener);
            Validate.notNull(to);

            this.id = id;
            this.data = data;
            this.listener = listener;
            this.to = to;
            this.messageId = messageId;
        }

        public Long getId() {
            return id;
        }

        public ByteBuffer getData() {
            return data;
        }

        public OutgoingMessageResponseListener getListener() {
            return listener;
        }

        public MessageId getMessageId() {
            return messageId;
        }

        public A getTo() {
            return to;
        }
    }

    private static final class OutgoingResponse<A> extends SendEntity {
        private Long id;
        private ByteBuffer data;
        private A destination;

        private OutgoingResponse(Long id, ByteBuffer data, A destination) {
            Validate.notNull(id);
            Validate.notNull(data);
            Validate.notNull(destination);
            this.id = id;
            this.data = data;
            this.destination = destination;
        }

        public Long getId() {
            return id;
        }

        public ByteBuffer getData() {
            return data;
        }

        public A getDestination() {
            return destination;
        }
    }
    
    /**
     * Return type of {@link #process(long) }. Contains timed out requests / the next timestamp a request will timeout / an
     * <b>estimation</b> of the number of packets available to be read.
     */
    public static final class OutgoingMessageManagerResult {
        private Collection<OutgoingMessageResponseListener> listenersForFailures;
        private long maxTimestamp;
        private int packetsAvailable;

        private OutgoingMessageManagerResult(Collection<OutgoingMessageResponseListener> listenersForFailures, long maxTimestamp,
                int packetsAvailable) {
            Validate.noNullElements(listenersForFailures);
            Validate.inclusiveBetween(0, Integer.MAX_VALUE, packetsAvailable);
            this.listenersForFailures = Collections.unmodifiableCollection(listenersForFailures);
            this.maxTimestamp = maxTimestamp;
            this.packetsAvailable = packetsAvailable;
        }

        /**
         * Get the responders for timed out requests.
         * @return responders for timed out requests
         */
        public Collection<OutgoingMessageResponseListener> getListenersForFailures() {
            return listenersForFailures;
        }

        /**
         * Get the next timestamp that a request will time out.
         * @return next timestamp that a request will time out
         */
        public long getMaxTimestamp() {
            return maxTimestamp;
        }

        /**
         * Get the <b>estimated</b> number of packets available to be read.
         * @return <b>estimated</b> number of packets available to be read
         */
        public int getPacketsAvailable() {
            return packetsAvailable;
        }
        
    }
}
