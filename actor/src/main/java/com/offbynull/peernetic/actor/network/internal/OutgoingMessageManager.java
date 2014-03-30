/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.actor.network.internal;

import com.offbynull.peernetic.actor.helpers.TimeoutManager;
import com.offbynull.peernetic.actor.helpers.TimeoutManager.TimeoutManagerResult;
import com.offbynull.peernetic.common.utils.ByteBufferUtils;
import com.offbynull.peernetic.actor.network.OutgoingFilter;
import com.offbynull.peernetic.actor.network.Serializer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Manages outgoing messages.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class OutgoingMessageManager<A> {
    private OutgoingFilter<A> outgoingFilter;
    private Serializer serializer;
    
    private TimeoutManager<OutMessage<A>> queuedSendsTimeoutManager = new TimeoutManager<>();
    private LinkedList<OutMessage<A>> queuedSends = new LinkedList<>();

    /**
     * Constructs a {@link OutgoingMessageManager} object.
     * @param outgoingFilter outgoing filter to use for messages
     * @param serializer serializer to use to serialize messages
     * @throws NullPointerException if any arguments are {@code null}
     */
    public OutgoingMessageManager(OutgoingFilter<A> outgoingFilter, Serializer serializer) {
        Validate.notNull(outgoingFilter);
        Validate.notNull(serializer);
        
        this.outgoingFilter = outgoingFilter;
        this.serializer = serializer;
    }
    
    /**
     * Called when a message is ready to go out.
     * @param to destination address
     * @param content content
     * @param queueTimestampTimeout maximum amount of time this message can take to go on the network before being discarded
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void queue(A to, Object content, long queueTimestampTimeout) {
        Validate.notNull(to);
        Validate.notNull(content);
        
        ByteBuffer data = serializer.serialize(content);
        
        ByteBuffer tempData = ByteBufferUtils.copyContents(data);
        try {
            tempData = outgoingFilter.filter(to, tempData);
        } catch (RuntimeException re) { // NOPMD
            return;
        }
        
        OutMessage<A> message = new OutMessage<>(to, tempData);
        
        queuedSends.add(message);
        queuedSendsTimeoutManager.add(message, queueTimestampTimeout);
    }

   /**
     * Flush all messages to go out.
     * @return all messages to go out
     */
    public Collection<OutMessage<A>> flush() {
        List<OutMessage<A>> messages = new ArrayList<>(queuedSends.size());

        OutMessage<A> message;
        while ((message = queuedSends.poll()) != null) {
            if (message.isCanceled()) {
                continue;
            }

            messages.add(message);
            queuedSendsTimeoutManager.cancel(message);
        }
        
        return Collections.unmodifiableCollection(messages);
    }
    
    /**
     * Get the next message go go out.
     * @return next message go go out, or {@code null} if no more messages are available
     */
    public OutMessage<A> getNext() {
        OutMessage message = null;
        while (true) {
            message = queuedSends.poll();
            
            if (message == null) {
                return null;
            }
            
            if (!message.isCanceled()) {
                break;
            }
            
            queuedSendsTimeoutManager.cancel(message);
        }
        
        return message;
    }

    /**
     * Checks to see if there are pending messages waiting to go out.
     * @return {@code true} if there are messages waiting to go out, {@code false} otherwise
     */
    public boolean hasMore() {
        return !queuedSends.isEmpty();
    }
    
    /**
     * Removes messages from the queue that have timed out.
     * @param timestamp current timestamp
     * @return next timeout timestamp
     */
    public long process(long timestamp) {
        TimeoutManagerResult<OutMessage<A>> result = queuedSendsTimeoutManager.process(timestamp);
        
        for (OutMessage<A> message : result.getTimedout()) {
            message.cancel();
        }
        
        return result.getNextTimeoutTimestamp();
    }

    /**
     * An outgoing message.
     * @param <A> address type
     */
    public static final class OutMessage<A> {
        private boolean canceled;
        private ByteBuffer data;
        private A to;

        private OutMessage(A to, ByteBuffer data) {
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
        
        private void cancel() {
            canceled = true;
        }

        private boolean isCanceled() {
            return canceled;
        }
    }
}
