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
package com.offbynull.peernetic.actor.network.internal;

import com.offbynull.peernetic.actor.helpers.TimeoutManager;
import com.offbynull.peernetic.actor.helpers.TimeoutManager.TimeoutManagerResult;
import com.offbynull.peernetic.common.utils.ByteBufferUtils;
import com.offbynull.peernetic.actor.network.Deserializer;
import com.offbynull.peernetic.actor.network.IncomingFilter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Manages incoming messages.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class IncomingMessageManager<A> {
    private IncomingFilter<A> incomingFilter;
    private Deserializer deserializer;

    private TimeoutManager<InMessage<A>> queuedRecvsTimeoutManager = new TimeoutManager<>();
    private LinkedList<InMessage<A>> queuedRecvs = new LinkedList<>();

    /**
     * Constructs a {@link IncomingMessageManager} object.
     * @param incomingFilter filter from use for incoming messages
     * @param deserializer deserializer from use for incoming messages
     * @throws NullPointerException if any argument is {@code null}
     */
    public IncomingMessageManager(IncomingFilter<A> incomingFilter, Deserializer deserializer) {
        Validate.notNull(incomingFilter);
        Validate.notNull(deserializer);
        
        this.incomingFilter = incomingFilter;
        this.deserializer = deserializer;
    }
    
    /**
     * Called when a message comes in.
     * @param from destination address
     * @param data request data
     * @param queueTimestampTimeout maximum amount of time this message can take from get read out before being discarded
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void queue(A from, ByteBuffer data, long queueTimestampTimeout) {
        Validate.notNull(from);
        Validate.notNull(data);
        
        ByteBuffer tempData = ByteBufferUtils.copyContents(data);
        try {
            tempData = incomingFilter.filter(from, tempData);
        } catch (RuntimeException re) { // NOPMD
            return;
        }
        
        Object content;
        try {
            content = deserializer.deserialize(tempData);
        } catch (IllegalArgumentException | IllegalStateException e) { // NOPMD
            // throws if deserialized item is null
            return;
        }
        
        InMessage<A> message = new InMessage<>(from, content);
        
        queuedRecvs.add(message);
        queuedRecvsTimeoutManager.add(message, queueTimestampTimeout);
    }
    
    /**
     * Removes messages from the queue that have timed out.
     * @param timestamp current timestamp
     * @return next timeout timestamp
     */
    public long process(long timestamp) {
        TimeoutManagerResult<InMessage<A>> result = queuedRecvsTimeoutManager.process(timestamp);
        
        for (InMessage<A> message : result.getTimedout()) {
            message.cancel();
        }
        
        return result.getNextTimeoutTimestamp();
    }

   /**
     * Flush all messages that have come in.
     * @return all messages that have come in
     */
    public Collection<InMessage<A>> flush() {
        List<InMessage<A>> messages = new ArrayList<>(queuedRecvs.size());

        InMessage<A> message;
        while ((message = queuedRecvs.poll()) != null) {
            if (message.isCanceled()) {
                continue;
            }

            messages.add(message);
            queuedRecvsTimeoutManager.cancel(message);
        }
        
        return Collections.unmodifiableCollection(messages);
    }
    
    /**
     * An outgoing message.
     * @param <A> address type
     */
    public static final class InMessage<A> {
        private boolean canceled;
        private Object content;
        private A from;

        private InMessage(A from, Object content) {
            Validate.notNull(content);
            Validate.notNull(from);

            this.content = content;
            this.from = from;
        }

        /**
         * Get data.
         * @return data
         */
        public Object getContent() {
            return content;
        }

        /**
         * Get source address.
         * @return source address
         */
        public A getFrom() {
            return from;
        }
        
        private void cancel() {
            canceled = true;
        }

        private boolean isCanceled() {
            return canceled;
        }
    }
}
