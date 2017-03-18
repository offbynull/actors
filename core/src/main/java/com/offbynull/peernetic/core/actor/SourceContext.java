/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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

import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * A source context is an implementation of {@link Context} that allows modification of properties that normally shouldn't be modifiable
 * (e.g. incomingMessage). Do not pass directly in to an actor, instead use {@link #toNormalContext() }.
 * 
 * @author Kasra Faghihi
 */
public final class SourceContext implements Context {
    private Address self;
    private Instant time;
    private Address source;
    private Address destination;
    private Object incomingMessage;
    private List<BatchedOutgoingMessage> outgoingMessages = new LinkedList<>();

    @Override
    public Address self() {
        return self;
    }

    /**
     * Sets the address of the actor that this context is for.
     * @param self address of actor
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code self} is empty
     */
    public void setSelf(Address self) {
        Validate.notNull(self);
        Validate.isTrue(!self.isEmpty());
        this.self = self;
    }

    @Override
    public Instant time() {
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
    public Address source() {
        return source;
    }

    /**
     * Set the address the incoming message was sent from.
     * @param source address of incoming message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} is empty
     */
    public void setSource(Address source) {
        Validate.notNull(source);
        Validate.isTrue(!source.isEmpty());
        this.source = source;
    }

    @Override
    public Address destination() {
        return destination;
    }

    /**
     * Set the address the incoming message was sent to.
     * @param destination address of incoming message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code destination} is empty
     */
    public void setDestination(Address destination) {
        Validate.notNull(destination);
        Validate.isTrue(!destination.isEmpty());
        this.destination = destination;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T in() {
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
    public void out(Address source, Address destination, Object message) {
        // sourceId can be null
        Validate.notNull(source);
        Validate.notNull(destination);
        Validate.notNull(message);
        Validate.isTrue(self.isPrefixOf(source));
        Validate.isTrue(!destination.isEmpty());
        outgoingMessages.add(new BatchedOutgoingMessage(source, destination, message));
    }
    
    @Override
    public List<BatchedOutgoingMessage> viewOuts() {
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
     * pass this context to an actor.
     * @return a wrapped version of this context that disables
     */
    public Context toNormalContext() {
        return new Context() {

            @Override
            public void out(Address source, Address destination, Object message) {
                SourceContext.this.out(source, destination, message);
            }
            
            @Override
            public List<BatchedOutgoingMessage> viewOuts() {
                return SourceContext.this.viewOuts();
            }

            @Override
            public Address destination() {
                return SourceContext.this.destination();
            }

            @Override
            public <T> T in() {
                return SourceContext.this.in();
            }

            @Override
            public Address self() {
                return SourceContext.this.self();
            }

            @Override
            public Address source() {
                return SourceContext.this.source();
            }

            @Override
            public Instant time() {
                return SourceContext.this.time();
            }
        };
    }
}
