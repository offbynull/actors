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
package com.offbynull.actors.gateways.actor;

import static com.offbynull.actors.gateway.CommonAddresses.DEFAULT_LOG_ADDRESS;
import static com.offbynull.actors.gateway.CommonAddresses.DEFAULT_TIMER_ADDRESS;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.actors.gateways.log.LogMessage;
import com.offbynull.actors.shuttle.Address;
import com.offbynull.coroutines.user.Continuation;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

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
public final class Context implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    static final Object DEFAULT_CHECKPOINT_PAYLOAD = new DefaultCheckpointMessage();
    static final long DEFAULT_CHECKPOINT_TIMEOUT = Long.MAX_VALUE;

    private RuleSet ruleSet;
    private Address self;
    private Instant time;
    private Address source;
    private Address destination;
    private Object in;
    private List<BatchedOutgoingMessageCommand> outs;
    private List<BatchedCreateRootCommand> newRoots;
    private List<BatchedCreateChildCommand> newChildren;
    
    private Map<Class<?>, ShortcircuitLogic> shortcircuits;
    private Object checkpointPayload;
    private long checkpointTimeout;
    private int checkpointInstance; // counter that will inc on checkpoint recovert -- older instances won't be stored
    private boolean checkpointUpdated; // checkpoint updated by the user
    
    private boolean intercept;
    private SuspendFlag flag;

    Context(Address self) {
        Validate.notNull(self);

        this.ruleSet = new RuleSet();
        this.self = self;
        this.outs = new LinkedList<>();
        this.newRoots = new LinkedList<>();
        this.newChildren = new LinkedList<>();
        
        this.shortcircuits = new HashMap<>();

        this.checkpointPayload = DEFAULT_CHECKPOINT_PAYLOAD;
        this.checkpointTimeout = DEFAULT_CHECKPOINT_TIMEOUT;
        this.checkpointUpdated = true;
        this.checkpointInstance = 0;
        
        this.flag = SuspendFlag.RELEASE;
        
        // Allow only messages from yourself -- priming messages always show up as coming from you
        ruleSet.rejectAll();
        ruleSet.allow(self, false);
    }

    Context(Context parentContext, String id) {
        Validate.notNull(parentContext);
        Validate.notNull(id);
        
        this.ruleSet = new RuleSet();
        this.self = parentContext.self.appendSuffix(id);
        this.outs = parentContext.outs;
        this.newRoots = parentContext.newRoots;
        this.newChildren = parentContext.newChildren;
        
        this.shortcircuits = new HashMap<>();
        
        this.checkpointPayload = DEFAULT_CHECKPOINT_PAYLOAD;
        this.checkpointTimeout = DEFAULT_CHECKPOINT_TIMEOUT;
        this.checkpointInstance = 0;
        
        this.flag = SuspendFlag.RELEASE;
        
        // Allow only messages from yourself -- priming messages always show up as coming from you
        ruleSet.rejectAll();
        ruleSet.allow(self, false);
    }
    
    /**
     * Get the address of the actor that this context is for.
     * @return address of actor
     */
    public Address self() {
        return self;
    }

    /**
     * Get the current time. This may not be exact, it is the time the actor was invoked.
     * @return current time
     */
    public Instant time() {
        return time;
    }

    /**
     * Add shortcircuit logic for incoming message types.
     * @param cls class to add shortcircuit logic for
     * @param shortcircuit shortcircuit logic (or {@code null} to remove the existing shortcircuit logic)
     * @throws NullPointerException if {@code cls} is {@code null}
     */
    public void shortcircuit(Class<?> cls, ShortcircuitLogic shortcircuit) {
        Validate.notNull(cls);
        
        if (shortcircuit == null) { // remove
            shortcircuits.remove(cls);
        } else {
            shortcircuits.put(cls, shortcircuit);
        }
    }

    /**
     * Set checkpoint message and time. Checkpoint message/time is only respected if the actor is a root actor.
     * @param payload payload to be used once checkpoint hits
     * @param timeout amount of time (in milliseconds) when the checkpoint hits and the actor rolls back
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code timeout} is negative
     */
    public void checkpoint(Object payload, long timeout) {
        Validate.notNull(payload);
        Validate.isTrue(timeout >= 0L);
        this.checkpointPayload = payload;
        this.checkpointTimeout = timeout;
        this.checkpointUpdated = true;
    }

    /**
     * Get the address the incoming message was sent from.
     * @return source address of incoming message
     */
    public Address source() {
        return source;
    }
   
    /**
     * Get the address the incoming message was sent to.
     * @return destination address of incoming message (absolute, not relative)
     */
    public Address destination() {
        return destination;
    }

    /**
     * Get the incoming message.
     * @param <T> message type
     * @return incoming message
     */
    @SuppressWarnings("unchecked")
    public <T> T in() {
        return (T) in;
    }
    
    /**
     * Get mode.
     * @return mode
     */
    public SuspendFlag mode() {
        return flag;
    }


    /**
     * Equivalent to calling {@code out(Address.fromString(destination), message)}. 
     * @param destination destination address
     * @param message outgoing message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code destination} is empty
     */
    public void out(String destination, Object message) {
        out(Address.fromString(destination), message);
    }

    /**
     * Equivalent to calling {@code out(self(), destination, message)}. 
     * @param destination destination address
     * @param message outgoing message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code destination} is empty
     */
    public void out(Address destination, Object message) {
        out(self(), destination, message);
    }

    /**
     * Equivalent to calling {@code out(self(), source(), message)}. 
     * @param message outgoing message
     * @throws NullPointerException if any argument is {@code null}
     */
    public void out(Object message) {
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
    public void out(String source, String destination, Object message) {
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
    public void out(Address source, Address destination, Object message) {
        Validate.notNull(source);
        Validate.notNull(destination);
        Validate.notNull(message);
        Validate.isTrue(self.isPrefixOf(source));
        Validate.isTrue(!destination.isEmpty());
        outs.add(new BatchedOutgoingMessageCommand(source, destination, message));
    }
    
    UnmodifiableList<BatchedOutgoingMessageCommand> viewOuts() {
        return (UnmodifiableList<BatchedOutgoingMessageCommand>) unmodifiableList(outs);
    }
    
    List<BatchedOutgoingMessageCommand> copyAndClearOutgoingMessages() {
        List<BatchedOutgoingMessageCommand> ret = new ArrayList<>(outs);
        outs.clear();
        
        return ret;
    }

    List<BatchedCreateRootCommand> copyAndClearNewRoots() {
        List<BatchedCreateRootCommand> ret = new ArrayList<>(newRoots);
        newRoots.clear();
        
        return ret;
    }

    List<BatchedCreateChildCommand> copyAndClearNewChildren() {
        List<BatchedCreateChildCommand> ret = new ArrayList<>(newChildren);
        newChildren.clear();
        
        return ret;
    }

    /**
     * Queue a root actor to be added.
     * <p>
     * The new root actor can't share state with this actor or its parent/child actors. It's a standalone actor that will run alongside this
     * actor.
     * <p>
     * If a actor runner already contains an actor with {@code id}, nothing happens.
     * @param id id of actor to add
     * @param coroutine coroutine being added
     * @param primingMessages messages to send to {@code actor} (shown as coming from itself) once its been added
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if {@code id} is empty
     */
    public void root(String id, Coroutine coroutine, Object... primingMessages) {
        Validate.notNull(id);
        Validate.notNull(coroutine);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        
        newRoots.add(new BatchedCreateRootCommand(id, coroutine, primingMessages));
    }

    /**
     * Queue a child actor to be added.
     * <p>
     * The new child actor can share state with this actor. If the parent actor ends, so will the child actor. The child actor's address is
     * {@code self().append(id)}.
     * <p>
     * Initially, the child actor will only be able to accept messages from its parent and itself.
     * @param id id of actor to add
     * @param coroutine coroutine being added
     * @param primingMessages messages to send to {@code actor} (shown as coming from itself) once its been added
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if {@code id} is empty
     */
    public void child(String id, Coroutine coroutine, Object... primingMessages) {
        Validate.notNull(id);
        Validate.notNull(coroutine);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        
        newChildren.add(new BatchedCreateChildCommand(this, id, coroutine, primingMessages));
    }

    /**
     * Intercept messages going to child actors. If the intercept flag is set, messages destined for children (and their children down the
     * chain) will first be sent to this actor (the parent). Should this actor choose to feed the message down the chain,
     * {@link #mode(com.offbynull.actors.gateways.actor.Context.SuspendFlag) } can be used.
     * @param intercept {@code true} to intercept messages to children, {@code false} otherwise
     */
    public void intercept(boolean intercept) {
        this.intercept = true;
    }

    /**
     * Sets the behavior to perform on next suspend. See {@link SuspendFlag} for more information.
     * <p>
     * Note that the mode being set here isn't retained. It gets reset to {@link SuspendFlag#RELEASE} after a call to
     * {@link Continuation#suspend()}.
     * @param flag behavior flag to apply on suspend
     * @throws NullPointerException if any argument is {@code null}
     */
    public void mode(SuspendFlag flag) {
        Validate.notNull(flag);
        this.flag = flag;
    }

    /**
     * Allow all incoming messages. All previously set allow/block rules are discarded.
     */
    public void allow() {
        ruleSet.allowAll();
    }

    /**
     * Allow incoming messages from some address. If no types are specified, all types are blocked.
     * <p>
     * Invoking this method overwrites any previous rule set for the address specified.
     * @param source source address to allow messages from
     * @param children if {@code true}, children of {@code source} will also be allowed
     * @param types types of messages to allow (child types aren't recognized)
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public void allow(Address source, boolean children, Class<?>... types) {
        ruleSet.allow(source, children, types);
    }
    
    /**
     * Equivalent to calling {@code allow(Address.fromString(source), children, types)}.
     * @param source source address to allow messages from
     * @param children if {@code true}, children of {@code source} will also be allowed
     * @param types types of messages to allow (child types aren't recognized)
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public void allow(String source, boolean children, Class<?>... types) {
        allow(Address.fromString(source), children, types);
    }

    /**
     * Block all incoming messages. All previously set allow/block rules are discarded.
     */
    public void block() {
        ruleSet.rejectAll();
    }

    /**
     * Block incoming messages from some address. If no types are specified, all types are blocked.
     * <p>
     * Invoking this method overwrites any previous rule set for the address specified.
     * @param source source address to block messages from
     * @param children if {@code true}, children of {@code source} will also be blocked
     * @param types types of messages to block (child types aren't recognized)
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public void block(Address source, boolean children, Class<?>... types) {
        ruleSet.reject(source, children, types);
    }

    /**
     * Equivalent to calling {@code block(Address.fromString(source), children, types)}.
     * @param source source address to block messages from
     * @param children if {@code true}, children of {@code source} will also be blocked
     * @param types types of messages to block (child types aren't recognized)
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public void block(String source, boolean children, Class<?>... types) {
        block(Address.fromString(source), children, types);
    }


    
    
    
    
    
    /**
     * Sends a timer request to the timer gateway located at address
     * {@link com.offbynull.actors.gateway.CommonAddresses#DEFAULT_TIMER_ADDRESS}.
     * @param delay delay in milliseconds
     * @param message message to have the timer reflect back after {@code delay}
     * @throws NullPointerException if any argument is {@code null}
     */
    public void timer(long delay, Object message) {
        out(DEFAULT_TIMER_ADDRESS.appendSuffix(Long.toString(delay)), message);
    }

    /**
     * Sends a error message to the log gateway located at address
     * {@link com.offbynull.actors.gateway.CommonAddresses#DEFAULT_LOG_ADDRESS}.
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     */
    public void logError(String message, Object... arguments) {
        out(DEFAULT_LOG_ADDRESS, LogMessage.error(message, arguments));
    }

    /**
     * Sends a warn message to the log gateway located at address
     * {@link com.offbynull.actors.gateway.CommonAddresses#DEFAULT_LOG_ADDRESS}.
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     */
    public void logWarn(String message, Object... arguments) {
        out(DEFAULT_LOG_ADDRESS, LogMessage.warn(message, arguments));
    }
    
    /**
     * Sends a info message to the log gateway located at address
     * {@link com.offbynull.actors.gateway.CommonAddresses#DEFAULT_LOG_ADDRESS}.
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     */
    public void logInfo(String message, Object... arguments) {
        out(DEFAULT_LOG_ADDRESS, LogMessage.info(message, arguments));
    }
    
    /**
     * Sends a debug message to the log gateway located at address
     * {@link com.offbynull.actors.gateway.CommonAddresses#DEFAULT_LOG_ADDRESS}.
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     */
    public void logDebug(String message, Object... arguments) {
        out(DEFAULT_LOG_ADDRESS, LogMessage.debug(message, arguments));
    }
    
    /**
     * Sends a trace message to the log gateway located at address
     * {@link com.offbynull.actors.gateway.CommonAddresses#DEFAULT_LOG_ADDRESS}.
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     */
    public void logTrace(String message, Object... arguments) {
        out(DEFAULT_LOG_ADDRESS, LogMessage.trace(message, arguments));
    }
    
    
    
    
    
    
    
    
    
    
    /**
     * Suspend mode.
     */
    public enum SuspendFlag {
        /**
         * Release control of the actor on suspend.
         */
        RELEASE,
        /**
         * Forward the message to child actors on suspend and release control after the message has been forwarded to child actors.
         */
        FORWARD_AND_RELEASE,
        /**
         * Forward the message to child actors on suspend and control is given back to forwarder once message has been forwarded to child
         * actors.
         */
        FORWARD_AND_RETURN
    }

    /**
     * Shortcircuit logic to perform.
     */
    public interface ShortcircuitLogic extends Serializable {
        /**
         * Perform shortcircuit logic.
         * @param ctx actor context
         * @return shortcircuit action
         */
        ShortcircuitAction perform(Context ctx);
    }
    
    /**
     * Action to perform after having shortcircuited an incoming message.
     */
    public enum ShortcircuitAction {
        /**
         * Execute the actor.
         */
        PROCESS,
        /**
         * Do not execute the actor.
         */
        PASS,
        /**
         * Terminate the actor.
         */
        TERMINATE
    }

    static final class BatchedCreateRootCommand implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String id;
        private final Coroutine coroutine;
        private final UnmodifiableList<Object> primingMessages;

        BatchedCreateRootCommand(String id, Coroutine coroutine, Object... primingMessages) {
            Validate.notNull(id);
            Validate.notNull(coroutine);
            Validate.notNull(primingMessages);
            Validate.noNullElements(primingMessages);
            this.id = id;
            this.coroutine = coroutine;
            this.primingMessages = (UnmodifiableList<Object>) unmodifiableList(new ArrayList<>(Arrays.asList(primingMessages)));
        }

        String getId() {
            return id;
        }

        Coroutine getCoroutine() {
            return coroutine;
        }

        UnmodifiableList<Object> getPrimingMessages() {
            return primingMessages;
        }
    }

    static final class BatchedCreateChildCommand implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Context fromContext;
        private final String id;
        private final Coroutine coroutine;
        private final UnmodifiableList<Object> primingMessages;

        BatchedCreateChildCommand(Context fromContext, String id, Coroutine coroutine, Object... primingMessages) {
            Validate.notNull(fromContext);
            Validate.notNull(id);
            Validate.notNull(coroutine);
            Validate.notNull(primingMessages);
            Validate.noNullElements(primingMessages);
            this.fromContext = fromContext;
            this.id = id;
            this.coroutine = coroutine;
            this.primingMessages = (UnmodifiableList<Object>) unmodifiableList(new ArrayList<>(Arrays.asList(primingMessages)));
        }

        Context fromContext() {
            return fromContext;
        }

        String getId() {
            return id;
        }

        Coroutine getCoroutine() {
            return coroutine;
        }

        UnmodifiableList<Object> getPrimingMessages() {
            return primingMessages;
        }
    }
    
    static final class BatchedOutgoingMessageCommand implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Address source;
        private final Address destination;
        private final Object message;

        BatchedOutgoingMessageCommand(Address source, Address destination, Object message) {
            Validate.notNull(source);
            Validate.notNull(destination);
            Validate.notNull(message);
            Validate.isTrue(!destination.isEmpty());
            this.source = source;
            this.destination = destination;
            this.message = message;
        }

        Address getSource() {
            return source;
        }

        Address getDestination() {
            return destination;
        }

        Object getMessage() {
            return message;
        }
    }
    
    
    
    
    
    
    
    
    
    
    void self(Address self) {
        this.self = self;
    }

    void time(Instant time) {
        this.time = time;
    }

    void source(Address source) {
        this.source = source;
    }

    void destination(Address destination) {
        this.destination = destination;
    }
    
    <T> void in(T in) {
        this.in = in;
    }

    List<BatchedOutgoingMessageCommand> outs() {
        return outs;
    }

    List<BatchedCreateRootCommand> newRoots() {
        return newRoots;
    }

    List<BatchedCreateChildCommand> newChildren() {
        return newChildren;
    }

    boolean intercept() {
        return intercept;
    }

    Map<Class<?>, ShortcircuitLogic> shortcircuits() {
        return shortcircuits;
    }

    void shortcircuits(Map<Class<?>, ShortcircuitLogic> shortcircuits) {
        this.shortcircuits = shortcircuits;
    }

    SuspendFlag flag() {
        return flag;
    }

    void flag(SuspendFlag flag) {
        this.flag = flag;
    }

    RuleSet ruleSet() {
        return ruleSet;
    }

    void ruleSet(RuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    void outs(List<BatchedOutgoingMessageCommand> outs) {
        this.outs = outs;
    }

    void newRoots(List<BatchedCreateRootCommand> newRoots) {
        this.newRoots = newRoots;
    }

    void newChildren(List<BatchedCreateChildCommand> newChildren) {
        this.newChildren = newChildren;
    }

    Object checkpointPayload() {
        return checkpointPayload;
    }

    long checkpointTimeout() {
        return checkpointTimeout;
    }

    void checkpointPayload(Object payload) {
        this.checkpointPayload = payload;
    }

    void checkpointTimeout(long time) {
        this.checkpointTimeout = time;
    }

    int checkpointInstance() {
        return checkpointInstance;
    }

    void checkpointInstance(int checkpointInstance) {
        this.checkpointInstance = checkpointInstance;
    }

    boolean checkpointUpdated() {
        return checkpointUpdated;
    }

    void checkpointUpdated(boolean checkpointUpdated) {
        this.checkpointUpdated = checkpointUpdated;
    }

    
    
    
    
    
    
    
    
    static final class DefaultCheckpointMessage implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
