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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * A source context is an implementation of {@link Context} that allows modification of properties that normally shouldn't be modifiable
 * (e.g. incomingMessage). Do not pass directly in to an {@link Actor}, instead use {@link #toNormalContext() }.
 * 
 * @author Kasra Faghihi
 */
public final class SourceContext implements Context {
    private String self;
    private Instant time;
    private String source;
    private String destination;
    private Object incomingMessage;
    private List<BatchedOutgoingMessage> outgoingMessages = new LinkedList<>();

    @Override
    public String getSelf() {
        return self;
    }

    /**
     * Sets the address of the actor that this context is for.
     * @param self address of actor
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code self} is empty
     */
    public void setSelf(String self) {
        Validate.notNull(self);
        Validate.notEmpty(self);
        this.self = self;
    }

    @Override
    public Instant getTime() {
        return time;
    }

    /**
     * Set the current time. This may not be exact, it is essentially just the time when the actor was triggered with new incoming message.
     * @param time current time
     * @throws NullPointerException if any argument is {@code null}
     */
    public void setTime(Instant time) {
        Validate.notNull(time);
        this.time = time;
    }

    @Override
    public String getSource() {
        return source;
    }

    /**
     * Set the address the incoming message was sent from.
     * @param source address of incoming message
     * @throws NullPointerException if any argument is {@code null}
     */
    public void setSource(String source) {
        Validate.notNull(source);
        this.source = source;
    }

    @Override
    public String getDestination() {
        return destination;
    }

    /**
     * Set the address the incoming message was sent to.
     * @param destination address of incoming message
     * @throws NullPointerException if any argument is {@code null}
     */
    public void setDestination(String destination) {
        Validate.notNull(destination);
        this.destination = destination;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getIncomingMessage() {
        return (T) incomingMessage;
    }

    /**
     * Set the incoming message.
     * @param incomingMessage incoming message
     * @throws NullPointerException if any argument is {@code null}
     */
    public void setIncomingMessage(Object incomingMessage) {
        Validate.notNull(incomingMessage);
        this.incomingMessage = incomingMessage;
    }

    @Override
    public void addOutgoingMessage(String destination, Object message) {
        addOutgoingMessage(null, destination, message);
    }

    @Override
    public void addOutgoingMessage(String sourceId, String destination, Object message) {
        // sourceId can be null
        Validate.notNull(destination);
        Validate.notNull(message);
        outgoingMessages.add(new BatchedOutgoingMessage(sourceId, destination, message));
    }
    
    @Override
    public List<BatchedOutgoingMessage> viewOutgoingMessages() {
        return Collections.unmodifiableList(outgoingMessages);
    }
    
    /**
     * Get a copy of the outgoing message queue and clear the original.
     * @return list of queued outgoing messages
     */
    public List<BatchedOutgoingMessage> copyAndClearOutgoingMessages() {
        List<BatchedOutgoingMessage> ret = new ArrayList<>(outgoingMessages);
        outgoingMessages.clear();
        
        return ret;
    }
    
    /**
     * Wraps this context in a new {@link Context} such that the setters aren't exposed / are hidden from access. Use this when you have to
     * pass this context to an {@link Actor}.
     * @return a wrapped version of this context that disables
     */
    public Context toNormalContext() {
        return new Context() {

            @Override
            public void addOutgoingMessage(String destination, Object message) {
                SourceContext.this.addOutgoingMessage(destination, message);
            }

            @Override
            public void addOutgoingMessage(String sourceId, String destination, Object message) {
                SourceContext.this.addOutgoingMessage(sourceId, destination, message);
            }
            
            @Override
            public List<BatchedOutgoingMessage> viewOutgoingMessages() {
                return SourceContext.this.viewOutgoingMessages();
            }

            @Override
            public String getDestination() {
                return SourceContext.this.getDestination();
            }

            @Override
            public <T> T getIncomingMessage() {
                return SourceContext.this.getIncomingMessage();
            }

            @Override
            public String getSelf() {
                return SourceContext.this.getSelf();
            }

            @Override
            public String getSource() {
                return SourceContext.this.getSource();
            }

            @Override
            public Instant getTime() {
                return SourceContext.this.getTime();
            }
        };
    }
}
