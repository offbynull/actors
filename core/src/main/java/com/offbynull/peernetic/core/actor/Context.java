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

import com.offbynull.coroutines.user.Continuation;
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
 * <li>set of rules that define what type of messages an actor can accept and from where.</li>
 * </ul>
 * 
 * By default, a context is set to block all incoming messages except those coming from itself.
 * 
 * @author Kasra Faghihi
 */
public interface Context {

    /**
     * Equivalent to calling {@code out(Address.fromString(destination), message)}. 
     * @param destination destination address
     * @param message outgoing message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code destination} is empty
     */
    default void out(String destination, Object message) {
        out(Address.fromString(destination), message);
    }

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
     * Equivalent to calling {@code out(self(), source(), message)}. 
     * @param message outgoing message
     * @throws NullPointerException if any argument is {@code null}
     */
    default void out(Object message) {
        out(self(), source(), message);
    }

    /**
     * Equivalent to calling {@code out(Address.fromString(source), Address.fromString(destination), message)}. 
     * @param source source address (must start with {@link #self()})
     * @param destination destination address
     * @param message outgoing message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code destination} is empty, or if {@code source} doesn't start with {@link #self()}
     */
    default void out(String source, String destination, Object message) {
        out(Address.fromString(source), Address.fromString(destination), message);
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
     * @param primingMessages messages to send to {@code actor} (shown as coming from itself) once its been added
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if {@code id} is empty, or if {@code id} is already a child
     */
    void child(String id, Coroutine actor, Object ... primingMessages);
    
    /**
     * Checks to see if a child actor exists.
     * @param id id of actor to check
     * @return {@code true} if the child actor exists, {@code false} otherwise
     * @throws NullPointerException if any argument is {@code null}
     */
    boolean isChild(String id);
    
    /**
     * Intercept messages going to child actors. If the intercept flag is set, messages destined for children (and their children down the
     * chain) will first be sent to this actor (the parent). Should this actor choose to feed the message down the chain,
     * {@link #forward() } can be used.
     * @param intercept {@code true} to intercept messages to children, {@code false} otherwise
     */
    void intercept(boolean intercept);
    
    /**
     * Sets message forwarding behaviour on next suspend. If set to anything other than {@link ForwardMode#DO_NOT_FORWARD}, the incoming
     * message will get fed to any child expecting it once {@link Continuation#suspend() } is invoked.
     * <p>
     * If mode is set to...
     * <ul>
     * <li>{@link ForwardMode#DO_NOT_FORWARD}, the message won't be forwarded to any child actors.</li>
     * <li>{@link ForwardMode#FORWARD_AND_FORGET}, the message will be forwarded to child actors.</li>
     * <li>{@link ForwardMode#FORWARD_AND_RETURN}, the message will be forwarded to child actors and control will be given back to this
     * actor once child actors are done with the message. {@link Continuation#suspend() } can then be invoked again to suspend this
     * actor.</li>
     * </ul>
     * Note that the forward mode being set here isn't retained. It gets reset to {@link ForwardMode#DO_NOT_FORWARD} after a forward.
     * <p>
     * An example...
     * <pre>
     * (Continuation cnt) -&gt; {
     *     Context ctx = (Context) cnt.getContext();
     *     ctx.allow();                      // Set flag to allow any message from any source
     *     ctx.intercept(true);              // Set flag to intercept incoming messages for children
     * 
     *     cnt.suspend();                    // Wait for incoming message
     * 
     *     Object msg = ctx.in();            // Get incoming message
     * 
     *     ctx.forward(FORWARD_AND_RETURN); // Set flag to forward msg to children on next suspend()
     * 
     *     ctx.logDebug("About to fwd!");
     *     cnt.suspend();                    // Forward to children
     *     ctx.logDebug("Done fwding!");
     * 
     *     cnt.suspend();                    // Suspend actor
     * }
     * </pre>
     * @param forwardMode forward mode
     * @throws NullPointerException if any argument is {@code null}
     */
    void forward(ForwardMode forwardMode);
    
    /**
     * Allow all incoming messages. All previously set allow/block rules are discarded.
     */
    void allow();

    /**
     * Allow incoming messages from some address. If no types are specified, all types are blocked.
     * <p>
     * Invoking this method overwrites any previous rule set for the address specified.
     * @param source source address to allow messages from
     * @param children if {@code true}, children of {@code source} will also be allowed
     * @param types types of messages to allow (child types aren't recognized)
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    void allow(Address source, boolean children, Class<?> ... types);
    
    /**
     * Equivalent to calling {@code allow(Address.fromString(source), children, types)}.
     * @param source source address to allow messages from
     * @param children if {@code true}, children of {@code source} will also be allowed
     * @param types types of messages to allow (child types aren't recognized)
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    default void allow(String source, boolean children, Class<?> ... types) {
        allow(Address.fromString(source), children, types);
    }

    /**
     * Block all incoming messages. All previously set allow/block rules are discarded.
     */
    void block();

    /**
     * Block incoming messages from some address. If no types are specified, all types are blocked.
     * <p>
     * Invoking this method overwrites any previous rule set for the address specified.
     * @param source source address to block messages from
     * @param children if {@code true}, children of {@code source} will also be blocked
     * @param types types of messages to block (child types aren't recognized)
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    void block(Address source, boolean children, Class<?> ... types);

    /**
     * Equivalent to calling {@code block(Address.fromString(source), children, types)}.
     * @param source source address to block messages from
     * @param children if {@code true}, children of {@code source} will also be blocked
     * @param types types of messages to block (child types aren't recognized)
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    default void block(String source, boolean children, Class<?> ... types) {
        block(Address.fromString(source), children, types);
    }
    
    /**
     * Sends a timer request to the timer gateway located at address
     * {@link com.offbynull.peernetic.core.common.DefaultAddresses#DEFAULT_TIMER_ADDRESS}.
     * @param delay delay in milliseconds
     * @param message message to have the timer reflect back after {@code delay}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} doesn't start with {@link #self()}
     */
    default void timer(long delay, Object message) {
        out(DEFAULT_TIMER_ADDRESS.appendSuffix(Long.toString(delay)), message);
    }

    /**
     * Sends a error message to the log gateway located at address
     * {@link com.offbynull.peernetic.core.common.DefaultAddresses#DEFAULT_LOG_ADDRESS}.
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} doesn't start with {@link #self()}
     */
    default void logError(String message, Object... arguments) {
        out(DEFAULT_LOG_ADDRESS, LogMessage.error(message, arguments));
    }

    /**
     * Sends a warn message to the log gateway located at address
     * {@link com.offbynull.peernetic.core.common.DefaultAddresses#DEFAULT_LOG_ADDRESS}.
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} doesn't start with {@link #self()}
     */
    default void logWarn(String message, Object... arguments) {
        out(DEFAULT_LOG_ADDRESS, LogMessage.warn(message, arguments));
    }
    
    /**
     * Sends a info message to the log gateway located at address
     * {@link com.offbynull.peernetic.core.common.DefaultAddresses#DEFAULT_LOG_ADDRESS}.
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} doesn't start with {@link #self()}
     */
    default void logInfo(String message, Object... arguments) {
        out(DEFAULT_LOG_ADDRESS, LogMessage.info(message, arguments));
    }
    
    /**
     * Sends a debug message to the log gateway located at address
     * {@link com.offbynull.peernetic.core.common.DefaultAddresses#DEFAULT_LOG_ADDRESS}.
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} doesn't start with {@link #self()}
     */
    default void logDebug(String message, Object... arguments) {
        out(DEFAULT_LOG_ADDRESS, LogMessage.debug(message, arguments));
    }
    
    /**
     * Sends a trace message to the log gateway located at address
     * {@link com.offbynull.peernetic.core.common.DefaultAddresses#DEFAULT_LOG_ADDRESS}.
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} doesn't start with {@link #self()}
     */
    default void logTrace(String message, Object... arguments) {
        out(DEFAULT_LOG_ADDRESS, LogMessage.trace(message, arguments));
    }
    
    /**
     * Forward mode.
     */
    public enum ForwardMode {
        /**
         * Do not forward message to child actors.
         */
        DO_NOT_FORWARD,
        /**
         * Forward the message to child actors.
         */
        FORWARD_AND_FORGET,
        /**
         * Forward the message to child actors and release control back to forwarder once child actors are done with the message.
         */
        FORWARD_AND_RETURN
    }
}
