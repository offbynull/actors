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
 * Queue of message responses.
 * <p/>
 * Responses to messages can be added in each {@link Actor#onStep(long, java.util.Iterator) } call.
 * @author Kasra Faghihi
 */
public final class ResponseQueue {
    private MultiMap<ActorQueueWriter, Message> queue = new MultiValueMap<>();
    private ActorQueueWriter selfWriter;

    ResponseQueue(ActorQueueWriter selfWriter) {
        Validate.notNull(selfWriter);
        
        this.selfWriter = selfWriter;
    }
    
    /**
     * Add a response.
     * @param original message response is for
     * @param response response message
     * @param respondable if the response message should be respondable
     * @return {@code true} if a response message was added, or {@code false} if a response couldn't be added (because {@code original} has
     * no {@link ActorQueueWriter} to respond to)
     * @throws NullPointerException if {@code response} is {@code null}
     */
    public boolean addResponse(Message original, Object response, boolean respondable) {
        Validate.notNull(response);
        
        ActorQueueWriter dst = original.getResponseWriter();
        if (dst == null) {
            return false;
        }
        
        Message message = new Message(selfWriter, response);
        queue.put(dst, message);
        
        return true;
    }
    
    void flush() {
        for (Entry<ActorQueueWriter, Object> entry : queue.entrySet()) {
            ActorQueueWriter respondTo = entry.getKey();
            Collection<Message> responses = (Collection<Message>) entry.getValue();
            
            respondTo.push(responses);
        }
        
        queue.clear();
    }
}
