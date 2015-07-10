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

import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Instant;
import java.util.List;

/**
 * Context of an actor. An actor's context is passed in to an actor each time an incoming message arrives. It contains ...
 * <ul>
 * <li>the address of the actor.</li>
 * <li>the time which the actor was triggered.</li>
 * <li>the incoming message that caused the actor to be triggered.</li>
 * <li>the outgoing messages that this actor is sending out.</li>
 * </ul>
 * 
 * @author Kasra Faghihi
 */
public interface Context {
    
    /**
     * Equivalent to calling {@code addOutgoingMessage(Address.of(), destination, message)}. 
     * @param destination destination address
     * @param message outgoing message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code destination} is empty
     */
    default void addOutgoingMessage(Address destination, Object message) {
        addOutgoingMessage(Address.of(), destination, message);
    }

    /**
     * Queue up an outgoing message.
     * @param sourceRelative address which this message is from (relative to {@link #getSelf()}). For example, if {@link #getSelf() }
     * returns "actor:1" and this is parameter is set to "id1:id2", the source address for the outgoing message being sent will be
     * "actor:1:id1:id2".
     * @param destination destination address
     * @param message outgoing message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code destination} is empty
     */
    void addOutgoingMessage(Address sourceRelative, Address destination, Object message);
    
    /**
     * Returns an unmodifiable list of outgoing messages. This list stays in sync as more outgoing messages are added.
     * @return unmodifiable list of outgoing messages
     */
    List<BatchedOutgoingMessage> viewOutgoingMessages();

    /**
     * Get the address the incoming message was sent to.
     * @return destination address of incoming message
     */
    Address getDestination();

    /**
     * Get the incoming message.
     * @param <T> message type
     * @return incoming message
     */
    @SuppressWarnings(value = "unchecked")
    <T> T getIncomingMessage();

    /**
     * Get the address of the actor that this context is for.
     * @return address of actor
     */
    Address getSelf();

    /**
     * Get the address the incoming message was sent from.
     * @return source address of incoming message
     */
    Address getSource();

    /**
     * Get the current time. This may not be exact, it is the time the actor was invoked.
     * @return current time
     */
    Instant getTime();
    
}
