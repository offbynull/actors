/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.core.actor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class Context {
    private String self;
    private Instant time;
    private String source;
    private String destination;
    private Object incomingMessage;
    private List<BatchedOutgoingMessage> outgoingMessages = new LinkedList<>();

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        Validate.notNull(self);
        Validate.notEmpty(self);
        this.self = self;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        Validate.notNull(time);
        this.time = time;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        Validate.notNull(source);
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        Validate.notNull(destination);
        this.destination = destination;
    }

    @SuppressWarnings("unchecked")
    public <T> T getIncomingMessage() {
        return (T) incomingMessage;
    }

    public void setIncomingMessage(Object incomingMessage) {
        Validate.notNull(incomingMessage);
        this.incomingMessage = incomingMessage;
    }

    public void addOutgoingMessage(String destination, Object message) {
        addOutgoingMessage(null, destination, message);
    }

    public void addOutgoingMessage(String sourceId, String destination, Object message) {
        // sourceId can be null
        Validate.notNull(destination);
        Validate.notNull(message);
        outgoingMessages.add(new BatchedOutgoingMessage(sourceId, destination, message));
    }
    
    public List<BatchedOutgoingMessage> copyAndClearOutgoingMessages() {
        List<BatchedOutgoingMessage> ret = new ArrayList<>(outgoingMessages);
        outgoingMessages.clear();
        
        return ret;
    }
    
    public static final class BatchedOutgoingMessage {
        private final String sourceId;
        private final String destination;
        private final Object message;

        public BatchedOutgoingMessage(String sourceId, String destination, Object message) {
            // sourceId may be null
            Validate.notNull(destination);
            Validate.notNull(message);
            Validate.notEmpty(destination);
            
            this.sourceId = sourceId;
            this.destination = destination;
            this.message = message;
        }

        public String getSourceId() {
            return sourceId;
        }

        public String getDestination() {
            return destination;
        }

        public Object getMessage() {
            return message;
        }        
    }
    
}
