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

import com.offbynull.peernetic.core.gateways.log.LogMessage;
import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Instant;
import java.util.List;

/**
 * Context of an {@link Actor}. An actor's context is passed in to an actor each time an incoming message arrives. It contains ...
 * <ul>
 * <li>address of the actor.</li>
 * <li>time which the actor was triggered.</li>
 * <li>incoming message that caused the actor to be triggered.</li>
 * <li>outgoing messages that this actor is sending out.</li>
 * </ul>
 * 
 * @author Kasra Faghihi
 */
public interface Context {
    
    /**
     * Default address to log gateway as String.
     */
    String DEFAULT_LOG_PREFIX = "log";
    
    /**
     * Default address to log gateway.
     */
    Address DEFAULT_LOG_PREFIX_ADDRESS = Address.of(DEFAULT_LOG_PREFIX);
    
    /**
     * Default address to timer gateway as String.
     */
    String DEFAULT_TIMER_PREFIX = "timer";
    
    /**
     * Default address to timer gateway.
     */
    Address DEFAULT_TIMER_PREFIX_ADDRESS = Address.of(DEFAULT_TIMER_PREFIX);
    
    /**
     * Equivalent to calling {@code out(Address.EMPTY, destination, message)}. 
     * @param destination destination address
     * @param message outgoing message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code destination} is empty
     */
    default void out(Address destination, Object message) {
        out(Address.EMPTY, destination, message);
    }

    /**
     * Queue up an outgoing message.
     * @param source source address, relative to {@link #self()}. For example, if {@link #self() } returns "actor:1" and this
     * parameter is set to "id1:id2", the source address for the outgoing message being sent will be "actor:1:id1:id2".
     * @param destination destination address
     * @param message outgoing message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code destination} is empty
     */
    void out(Address source, Address destination, Object message);
    
    /**
     * Returns an unmodifiable list of outgoing messages. This list stays in sync as more outgoing messages are added.
     * @return unmodifiable list of outgoing messages
     */
    List<BatchedOutgoingMessage> viewOuts();

    /**
     * Get the address the incoming message was sent to.
     * @return destination address of incoming message (absolute, not relative)
     */
    Address destination();

    /**
     * Get the incoming message.
     * @param <T> message type
     * @return incoming message
     */
    <T> T in();

    /**
     * Get the address of the actor that this context is for.
     * @return address of actor
     */
    Address self();

    /**
     * Get the address the incoming message was sent from.
     * @return source address of incoming message
     */
    Address source();

    /**
     * Get the current time. This may not be exact, it is the time the actor was invoked.
     * @return current time
     */
    Instant time();
    
    /**
     * Sends a timer request to the timer gateway located at address {@code timer}.
     * @param from source address, relative to {@link #self()}. For example, if {@link #self() } returns "actor:1" and this
     * parameter is set to "id1:id2", the source address for the outgoing message being sent will be "actor:1:id1:id2".
     * @param delay delay in milliseconds
     * @param message message to have the timer reflect back after {@code delay}
     * @throws NullPointerException if any argument is {@code null}
     */
    default void timer(Address from, long delay, Object message) {
        out(from, DEFAULT_TIMER_PREFIX_ADDRESS.appendSuffix(Long.toString(delay)), message);
    }

    /**
     * Sends a error message to the log gateway located at address {@code log}.
     * @param from source address, relative to {@link #self()}. For example, if {@link #self() } returns "actor:1" and this
     * parameter is set to "id1:id2", the source address for the outgoing message being sent will be "actor:1:id1:id2".
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     */
    default void logError(Address from, String message, Object... arguments) {
        out(from, DEFAULT_LOG_PREFIX_ADDRESS, LogMessage.error(message, arguments));
    }

    /**
     * Sends a warn message to the log gateway located at address {@code log}.
     * @param from source address, relative to {@link #self()}. For example, if {@link #self() } returns "actor:1" and this
     * parameter is set to "id1:id2", the source address for the outgoing message being sent will be "actor:1:id1:id2".
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     */
    default void logWarn(Address from, String message, Object... arguments) {
        out(from, DEFAULT_LOG_PREFIX_ADDRESS, LogMessage.warn(message, arguments));
    }
    
    /**
     * Sends a info message to the log gateway located at address {@code log}.
     * @param from source address, relative to {@link #self()}. For example, if {@link #self() } returns "actor:1" and this
     * parameter is set to "id1:id2", the source address for the outgoing message being sent will be "actor:1:id1:id2".
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     */
    default void logInfo(Address from, String message, Object... arguments) {
        out(from, DEFAULT_LOG_PREFIX_ADDRESS, LogMessage.info(message, arguments));
    }
    
    /**
     * Sends a debug message to the log gateway located at address {@code log}.
     * @param from source address, relative to {@link #self()}. For example, if {@link #self() } returns "actor:1" and this
     * parameter is set to "id1:id2", the source address for the outgoing message being sent will be "actor:1:id1:id2".
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     */
    default void logDebug(Address from, String message, Object... arguments) {
        out(from, DEFAULT_LOG_PREFIX_ADDRESS, LogMessage.debug(message, arguments));
    }
    
    /**
     * Sends a trace message to the log gateway located at address {@code log}.
     * @param from source address, relative to {@link #self()}. For example, if {@link #self() } returns "actor:1" and this
     * parameter is set to "id1:id2", the source address for the outgoing message being sent will be "actor:1:id1:id2".
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     */
    default void logTrace(Address from, String message, Object... arguments) {
        out(from, DEFAULT_LOG_PREFIX_ADDRESS, LogMessage.trace(message, arguments));
    }
}
