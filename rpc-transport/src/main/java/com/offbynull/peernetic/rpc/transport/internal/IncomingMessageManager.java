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
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;

/**
 * Manages incoming messages.
 * <p/>
 * When a new packet comes in, pipe it in to {@link #incomingData(long, java.lang.Object, java.nio.ByteBuffer, long) }. If the packet is
 * determined to be a request, it'll be returned on the next invokation of {@link #process(long) } and remain tracked internally until a
 * response is ready {@link #responseFormed(long) } or until a timeout has been reached. If the packet is determined to be a response, it'll
 * be returned on the next invokation of {@link #process(long) } and discarded.
 * <p/>
 * A {@link MessageIdCache} is used to remove duplicate requests / responses.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class IncomingMessageManager<A> {
    private IncomingFilter<A> incomingFilter;
    private MessageIdCache<A> messageIdCache;

    private TimeoutManager<Long> pendingRequestTimeoutManager = new TimeoutManager<>();
    private Map<Long, IncomingRequestInfo<A>> pendingRequestLookup = new HashMap<>();
    
    private List<IncomingRequest<A>> newRequests = new ArrayList<>();
    private List<IncomingResponse<A>> newResponses = new ArrayList<>();

    /**
     * Constructs a {@link IncomingMessageManager} object.
     * @param cacheSize number of message ids to cache (helps with preventing duplicate packets)
     * @param incomingFilter filter to use for incoming messages
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any numeric argument is negative
     */
    public IncomingMessageManager(int cacheSize, IncomingFilter<A> incomingFilter) {
        Validate.notNull(incomingFilter);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, cacheSize);
        
        this.incomingFilter = incomingFilter;
        messageIdCache = new MessageIdCache<>(cacheSize);
    }
    
    /**
     * Called when data arrives.
     * <p/>
     * If the data block is identified as a request, it'll get queued to be returned in the next {@link #process(long) } call. Generally,
     * a request needs a response. To send out a response, you need access to the message id of the request. The message id is stored
     * internally in this object until the time hits {@code maxTimestamp} or until {@link #responseFormed(long) }.
     * {@link #responseFormed(long) } will return an object with the required details to send back a respond to the request (return address,
     * message id, etc..).
     * <p/>
     * If the block is identified as a response, it'll get queued to be returned in the next {@link #process(long) } call. Unlike requests,
     * once it has been returned, internal references to it will be removed.
     * <p/>
     * If either the request or response is identified as a duplicate, it's discarded.
     * @param id internal id to set for the request / response
     * @param from sender
     * @param data raw message
     * @param maxTimestamp maximum timestamp to hold on to this packet before ignoring it 
     * @throws NullPointerException if any argument is {@code null}
     */
    public void incomingData(long id, A from, ByteBuffer data, long maxTimestamp) {
        Validate.notNull(from);
        Validate.notNull(data);
        
        ByteBuffer localTempBuffer = incomingFilter.filter(from, data);
        
        if (localTempBuffer == data) {
            localTempBuffer = ByteBufferUtils.copyContents(localTempBuffer);
        }

        MessageType messageType = MessageMarker.readMarker(localTempBuffer);
        if (messageType == null) {
            return;
        }
        
        switch (messageType) {
            case REQUEST: {
                MessageId messageId = MessageId.readId(localTempBuffer);

                if (!messageIdCache.add(from, messageId, MessageType.REQUEST)) {
                    //throw new RuntimeException("Duplicate messageid encountered");
                    return;
                }

                IncomingRequest<A> request = new IncomingRequest<>(id, from, localTempBuffer, messageId);
                IncomingRequestInfo<A> pending = new IncomingRequestInfo<>(id, from, messageId);

                pendingRequestTimeoutManager.add(id, maxTimestamp);
                pendingRequestLookup.put(id, pending);
                newRequests.add(request);
                break;
            }
            case RESPONSE: {
                MessageId messageId = MessageId.readId(localTempBuffer);

                if (!messageIdCache.add(from, messageId, MessageType.RESPONSE)) {
                    //throw new RuntimeException("Duplicate messageid encountered");
                    return;
                }

                IncomingResponse<A> response = new IncomingResponse<>(id, from, localTempBuffer, messageId);
                newResponses.add(response);
                break;
            }
            default:
                throw new IllegalStateException();
        }
    }
    
    /**
     * Signals that a response has been formed to an incoming request. Returns details to send back the response and removes internal
     * references to the request.
     * @param id internal id of the request
     * @return a {@link IncomingRequestInfo} object that provides the sender's address and the message id of the original request
     */
    public IncomingRequestInfo<A> responseFormed(long id) {
        pendingRequestTimeoutManager.cancel(id);
        return pendingRequestLookup.remove(id);
    }
    
    /**
     * Called once per iteration. Returns new requests, responses, timed out requests, and the next timestamp this method should be called
     * again.
     * @param timestamp current timestamp
     * @return new request/responses, timed out requests, and the next time this method should be called again
     */
    public IncomingPacketManagerResult<A> process(long timestamp) {
        TimeoutManagerResult<Long> results = pendingRequestTimeoutManager.process(timestamp);
        Set<Long> timedOutIds = results.getTimedout();
        
        Set<IncomingRequestInfo<A>> timedOutPendingRequests = new HashSet<>();
        for (Long timedOutId : timedOutIds) {
            timedOutPendingRequests.add(pendingRequestLookup.remove(timedOutId));
        }
        
        IncomingPacketManagerResult<A> ret = new IncomingPacketManagerResult<>(
                newRequests,
                timedOutPendingRequests,
                newResponses,
                results.getNextMaxTimestamp());
        
        newRequests = new LinkedList<>();
        newResponses = new LinkedList<>();
        
        return ret;
    }
    
    private interface ReceiveEntity {
        
    }

    /**
     * Incoming request.
     * @param <A> address type 
     */
    public static final class IncomingRequest<A> implements ReceiveEntity {
        private long id;
        private A from;
        private ByteBuffer data;
        private MessageId messageId;

        private IncomingRequest(long id, A from, ByteBuffer data, MessageId messageId) {
            Validate.notNull(from);
            Validate.notNull(data);
            Validate.notNull(messageId);
            
            this.id = id;
            this.from = from;
            this.data = data;
            this.messageId = messageId;
        }

        /**
         * Get the source address.
         * @return source address
         */
        public A getFrom() {
            return from;
        }

        /**
         * Get the request message.
         * @return message
         */
        public ByteBuffer getData() {
            return data;
        }

        /**
         * Get the message id.
         * @return message id
         */
        public MessageId getMessageId() {
            return messageId;
        }

        /**
         * Get the internal id.
         * @return internal id
         */
        public long getId() {
            return id;
        }
    }

    /**
     * Incoming request info.
     * @param <A> address type
     */
    public static final class IncomingRequestInfo<A> implements ReceiveEntity {
        private long id;
        private A from;
        private MessageId messageId;

        private IncomingRequestInfo(long id, A from, MessageId messageId) {
            Validate.notNull(from);
            Validate.notNull(messageId);
            
            this.id = id;
            this.from = from;
            this.messageId = messageId;
        }

        /**
         * Get the source address.
         * @return source address
         */
        public A getFrom() {
            return from;
        }

        /**
         * Get the message id.
         * @return message id
         */
        public MessageId getMessageId() {
            return messageId;
        }

        /**
         * Get the internal id.
         * @return internal id
         */
        public long getId() {
            return id;
        }
    }
    
    /**
     * Incoming response.
     * @param <A> address type
     */
    public static final class IncomingResponse<A> implements ReceiveEntity {
        private long id;
        private A from;
        private ByteBuffer data;
        private MessageId messageId;

        private IncomingResponse(long id, A from, ByteBuffer data, MessageId messageId) {
            Validate.notNull(from);
            Validate.notNull(data);
            Validate.notNull(messageId);
            
            this.id = id;
            this.from = from;
            this.data = data;
            this.messageId = messageId;
        }

        /**
         * Get the source address.
         * @return source address
         */
        public A getFrom() {
            return from;
        }

        /**
         * Get the request message.
         * @return message
         */
        public ByteBuffer getData() {
            return data;
        }

        /**
         * Get the message id.
         * @return message id
         */
        public MessageId getMessageId() {
            return messageId;
        }

        /**
         * Get the internal id.
         * @return internal id
         */
        public long getId() {
            return id;
        }
    }
    
    /**
     * Return type of {@link #process(long) }. Contains new requests / new responses / timed out requests / the next timestamp a request
     * will timeout.
     * @param <A> 
     */
    public static final class IncomingPacketManagerResult<A> {
        private Collection<IncomingRequest<A>> newIncomingRequests;
        private Collection<IncomingRequestInfo<A>> timedOutIncomingRequests;
        private Collection<IncomingResponse<A>> newIncomingResponses;
        private long maxTimestamp;

        private IncomingPacketManagerResult(Collection<IncomingRequest<A>> newIncomingRequests,
                Collection<IncomingRequestInfo<A>> timedOutIncomingRequests,
                Collection<IncomingResponse<A>> newIncomingResponses,
                long maxTimestamp) {
            Validate.noNullElements(newIncomingRequests);
            Validate.noNullElements(timedOutIncomingRequests);
            Validate.noNullElements(newIncomingResponses);
            
            this.newIncomingRequests = Collections.unmodifiableCollection(newIncomingRequests);
            this.timedOutIncomingRequests = Collections.unmodifiableCollection(timedOutIncomingRequests);
            this.newIncomingResponses = Collections.unmodifiableCollection(newIncomingResponses);
            this.maxTimestamp = maxTimestamp;
        }

        /**
         * Get queued incoming requests.
         * @return queued incoming requests
         */
        public Collection<IncomingRequest<A>> getNewIncomingRequests() {
            return newIncomingRequests;
        }

        /**
         * Get timed out incoming requests.
         * @return timed out incoming responses
         */
        public Collection<IncomingRequestInfo<A>> getTimedOutIncomingRequests() {
            return timedOutIncomingRequests;
        }

        /**
         * Get queued incoming responses.
         * @return queued incoming responses
         */
        public Collection<IncomingResponse<A>> getNewIncomingResponses() {
            return newIncomingResponses;
        }

        /**
         * Get the next timestamp that a request will time out.
         * @return next timestamp that a request will time out
         */
        public long getMaxTimestamp() {
            return maxTimestamp;
        }

    }
}
