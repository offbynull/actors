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
package com.offbynull.peernetic.common.concurrent.actor;

import java.util.Collection;
import java.util.Map.Entry;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.Validate;

/**
 * Queue of messages to be sent out.
 * <p/>
 * Messages get queued up in each {@link Actor#onStep(long, java.util.Iterator) } invocation and are sent out once the invocation completes.
 * @author Kasra Faghihi
 */
public final class PushQueue {
    private MultiMap<ActorQueueWriter, Message> queue = new MultiValueMap<>();
    private ActorQueueWriter self;

    PushQueue(ActorQueueWriter selfWriter) {
        Validate.notNull(selfWriter);
        
        this.self = selfWriter;
    }
    
    /**
     * Queue an outgoing response.
     * @param dst key for the message this message is responding to
     * @param id id for response
     * @param response response message content
     * @throws NullPointerException if any argument is {@code null}
     */
    public void queueResponseMessage(ActorQueueWriter dst, Object id, Object response) {
        Validate.notNull(dst);
        Validate.notNull(id);
        Validate.notNull(response);
        
        Message responseMsg;
        responseMsg = Message.createResponseMessage(id, response);
        
        queue.put(dst, responseMsg);
    }

    /**
     * Queue an outgoing message that doesn't expect a response.
     * @param dst outgoing writer
     * @param content message content
     * @throws NullPointerException if any argument is {@code null}
     */
    public void queueOneWayMessage(ActorQueueWriter dst, Object content) {
        Validate.notNull(dst);
        Validate.notNull(content);
        
        Message message = Message.createOneWayMessage(content);
        queue.put(dst, message);
    }

    /**
     * Queue an outgoing message that can be responded to.
     * @param dst outgoing writer
     * @param key key for this message
     * @param content message content
     * @throws NullPointerException if any argument is {@code null}
     */
    public void queueRequestMessage(ActorQueueWriter dst, Object key, Object content) {
        Validate.notNull(dst);
        Validate.notNull(key);
        Validate.notNull(content);
        
        Message message = Message.createRequestMessage(key, self, content);
        queue.put(dst, message);
    }

    void flush() {
        for (Entry<ActorQueueWriter, Object> entry : queue.entrySet()) {
            ActorQueueWriter respondTo = entry.getKey();
            Collection<Message> responses = (Collection<Message>) entry.getValue();
            
            respondTo.push(responses);
        }
        
        queue.clear();
    }

    MultiMap<ActorQueueWriter, Message> get() {
        return queue;
    }
}
