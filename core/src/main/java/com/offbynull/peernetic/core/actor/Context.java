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

import com.offbynull.coroutines.user.Coroutine;
import static com.offbynull.peernetic.core.common.DefaultAddresses.DEFAULT_LOG_ADDRESS;
import static com.offbynull.peernetic.core.common.DefaultAddresses.DEFAULT_TIMER_ADDRESS;
import com.offbynull.peernetic.core.gateways.log.LogMessage;
import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Instant;
import java.util.List;

/**
 * Context of an actor. An actor's context is passed in to an actor each time an incoming message arrives. It contains ...
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
     * Equivalent to calling {@code out(self(), destination, message)}. 
     * @param destination destination address
     * @param message outgoing message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code destination} is empty
     */
    default void out(Address destination, Object message) {
        out(self(), destination, message);
    }

    /**
     * Queue up an outgoing message.
     * @param source source address (must start with {@link #self()})
     * @param destination destination address
     * @param message outgoing message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code destination} is empty, or if {@code source} doesn't start with {@link #self()}
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
     * Add a child actor.
     * <p>
     * The new child actor can share state with the parent actor (the invoker of this method). If the parent actor ends,
     * so will the child actor. The child actor's address is {@code self().append(id)}.
     * <p>
     * Initially, the child actor will only be able to accept messages from its parent and itself.
     * @param id id of actor to add
     * @param actor actor being added
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code id} is empty, or if {@code id} is already a child
     */
    void spawnChild(String id, Coroutine actor);
    
//    /**
//     * Remove a child actor. If this actor doesn't contain a child with the id {@code id}, this method does nothing.
//     * @param id id of actor to remove
//     * @throws NullPointerException if any argument is {@code null}
//     */
//    void destroyChild(String id);
//    
//    /**
//     * Checks to see if a child actor exists.
//     * @param id id of actor to check
//     * @return {@code true} if the child actor exists, {@code false} otherwise
//     * @throws NullPointerException if any argument is {@code null}
//     */
//    boolean containsChild(String id);
//    
//    /**
//     * Allow any message from any source.
//     */
//    void allow();
//    
//    /**
//     * Allow any message type from {@code source}.
//     * @param source source address to allow messages from
//     * @throws NullPointerException if any argument is {@code null}
//     */
//    void allow(Address source);
//
//    /**
//     * Allow message of type {@code type} from {@code source}.
//     * @param source source address to allow messages from
//     * @param type type of messages to allow (child types aren't recognized)
//     * @throws NullPointerException if any argument is {@code null}
//     */
//    void allow(Address source, Class<?> type);
//
//    /**
//     * Block messages from any source.
//     */
//    void block();
//    
//    /**
//     * Block messages of any type from {@code source}.
//     * @param source source address to block messages from
//     * @throws NullPointerException if any argument is {@code null}
//     */
//    void block(Address source);
//
//    /**
//     * Block messages of type {@code type} from {@code source}.
//     * @param source source address to block messages from
//     * @param type type of messages to block (child types aren't recognized)
//     * @throws NullPointerException if any argument is {@code null}
//     */
//    void block(Address source, Class<?> type);
    
    /**
     * Sends a timer request to the timer gateway located at address
     * {@link com.offbynull.peernetic.core.common.DefaultAddresses#DEFAULT_TIMER_ADDRESS}.
     * @param source source address (must start with {@link #self()})
     * @param delay delay in milliseconds
     * @param message message to have the timer reflect back after {@code delay}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} doesn't start with {@link #self()}
     */
    default void timer(Address source, long delay, Object message) {
        out(source, DEFAULT_TIMER_ADDRESS.appendSuffix(Long.toString(delay)), message);
    }

    /**
     * Sends a error message to the log gateway located at address
     * {@link com.offbynull.peernetic.core.common.DefaultAddresses#DEFAULT_LOG_ADDRESS}.
     * @param source source address (must start with {@link #self()})
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} doesn't start with {@link #self()}
     */
    default void logError(Address source, String message, Object... arguments) {
        out(source, DEFAULT_LOG_ADDRESS, LogMessage.error(message, arguments));
    }

    /**
     * Sends a warn message to the log gateway located at address
     * {@link com.offbynull.peernetic.core.common.DefaultAddresses#DEFAULT_LOG_ADDRESS}.
     * @param source source address (must start with {@link #self()})
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} doesn't start with {@link #self()}
     */
    default void logWarn(Address source, String message, Object... arguments) {
        out(source, DEFAULT_LOG_ADDRESS, LogMessage.warn(message, arguments));
    }
    
    /**
     * Sends a info message to the log gateway located at address
     * {@link com.offbynull.peernetic.core.common.DefaultAddresses#DEFAULT_LOG_ADDRESS}.
     * @param source source address (must start with {@link #self()})
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} doesn't start with {@link #self()}
     */
    default void logInfo(Address source, String message, Object... arguments) {
        out(source, DEFAULT_LOG_ADDRESS, LogMessage.info(message, arguments));
    }
    
    /**
     * Sends a debug message to the log gateway located at address
     * {@link com.offbynull.peernetic.core.common.DefaultAddresses#DEFAULT_LOG_ADDRESS}.
     * @param source source address (must start with {@link #self()})
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} doesn't start with {@link #self()}
     */
    default void logDebug(Address source, String message, Object... arguments) {
        out(source, DEFAULT_LOG_ADDRESS, LogMessage.debug(message, arguments));
    }
    
    /**
     * Sends a trace message to the log gateway located at address
     * {@link com.offbynull.peernetic.core.common.DefaultAddresses#DEFAULT_LOG_ADDRESS}.
     * @param source source address (must start with {@link #self()})
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} doesn't start with {@link #self()}
     */
    default void logTrace(Address source, String message, Object... arguments) {
        out(source, DEFAULT_LOG_ADDRESS, LogMessage.trace(message, arguments));
    }
}
