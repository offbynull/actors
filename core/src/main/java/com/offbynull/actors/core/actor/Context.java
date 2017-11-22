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
package com.offbynull.actors.core.actor;

import static com.offbynull.actors.core.gateways.log.LogGateway.DEFAULT_LOG_ADDRESS;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.actors.core.gateways.log.LogMessage;
import static com.offbynull.actors.core.gateways.timer.TimerGateway.DEFAULT_TIMER_ADDRESS;
import com.offbynull.actors.core.shuttle.Address;
import com.offbynull.coroutines.user.Continuation;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    
    private Context parent;
    private CoroutineRunner runner;
    private RuleSet ruleSet;
    private Address self;
    private Instant time;
    private Address source;
    private Address destination;
    private Object in;
    private List<BatchedOutgoingMessage> outs;
    private List<BatchedCreateActorCommand> newRoots;
    private Map<String, Context> children;
    
    private Map<Class<?>, ShortcircuitLogic> shortcircuits;
    
    private boolean intercept;
    private CheckpointRestoreLogic checkpointRestoreLogic;
    private SuspendFlag flag;

    /**
     * Constructs a {@link Context} object.
     * @param runner actor runner
     * @param self self address
     * @throws NullPointerException if any argument is {@code null}
     */
    public Context(CoroutineRunner runner, Address self) {
        Validate.notNull(runner);
        Validate.notNull(self);

        this.ruleSet = new RuleSet();
        this.runner = runner;
        this.self = self;
        this.outs = new LinkedList<>();
        this.newRoots = new LinkedList<>();
        this.children = new HashMap<>();
        
        this.shortcircuits = new HashMap<>();
        
        this.flag = SuspendFlag.RELEASE;
        
        // Allow only messages from yourself -- priming messages always show up as coming from you
        ruleSet.rejectAll();
        ruleSet.allow(self, false);
    }
    
    /**
     * Check to see if this actor is a root actor (not a child of some other actor).
     * @return {@code true} if root actor, {@code false} if child actor
     */
    public boolean isRoot() {
        return parent == null;
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
        outs.add(new BatchedOutgoingMessage(source, destination, message));
    }
    
    /**
     * Returns an unmodifiable list of outgoing messages. This list stays in sync as more outgoing messages are added.
     * @return unmodifiable list of outgoing messages
     */
    public List<BatchedOutgoingMessage> viewOuts() {
        return Collections.unmodifiableList(outs);
    }
    
    /**
     * Get a copy of the outgoing message queue and clear the original.
     * @return list of queued outgoing messages
     */
    public List<BatchedOutgoingMessage> copyAndClearOutgoingMessages() {
        List<BatchedOutgoingMessage> ret = new ArrayList<>(outs);
        outs.clear();
        
        return ret;
    }
    
    /**
     * Get a copy of the new root actors queue and clear the original.
     * @return list of new root actors to create
     */
    public List<BatchedCreateActorCommand> copyAndClearNewRoots() {
        List<BatchedCreateActorCommand> ret = new ArrayList<>(newRoots);
        newRoots.clear();
        
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
     * @param actor actor being added
     * @param primingMessages messages to send to {@code actor} (shown as coming from itself) once its been added
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if {@code id} is empty
     */
    public void neighbour(String id, Coroutine actor, Object... primingMessages) {
        Validate.notNull(id);
        Validate.notNull(actor);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        
        newRoots.add(new BatchedCreateActorCommand(id, actor, primingMessages));
    }

    /**
     * Add a child actor.
     * <p>
     * The new child actor can share state with this actor. If the parent actor ends, so will the child actor. The child actor's address is
     * {@code self().append(id)}.
     * <p>
     * Initially, the child actor will only be able to accept messages from its parent and itself.
     * @param id id of actor to add
     * @param actor actor being added
     * @param primingMessages messages to send to {@code actor} (shown as coming from itself) once its been added
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if {@code id} is empty, or if {@code id} is already a child
     */
    public void child(String id, Coroutine actor, Object... primingMessages) {
        Validate.notNull(id);
        Validate.notNull(actor);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        
        Address childSelf = self().appendSuffix(id);
        CoroutineRunner childActorRunner = new CoroutineRunner(actor);
        
        Context childCtx = new Context(childActorRunner, childSelf);
        childCtx.parent = this;
        childCtx.outs = outs; // all outgoing messages go in to the same queue
        
        for (Object primingMessage : primingMessages) {
            childCtx.out(childSelf, childSelf, primingMessage);
        }
        
        childActorRunner.setContext(childCtx);
        
        Context existingCtx = children.putIfAbsent(id, childCtx);
        Validate.isTrue(existingCtx == null);
    }

    /**
     * Checks to see if a child actor exists.
     * @param id id of actor to check
     * @return {@code true} if the child actor exists, {@code false} otherwise
     * @throws NullPointerException if any argument is {@code null}
     */
    public boolean isChild(String id) {
        Validate.notNull(id);
        return children.containsKey(id);
    }

    /**
     * Intercept messages going to child actors. If the intercept flag is set, messages destined for children (and their children down the
     * chain) will first be sent to this actor (the parent). Should this actor choose to feed the message down the chain,
     * {@link #mode(com.offbynull.actors.core.context.Context.SuspendFlag...) } can be used.
     * @param intercept {@code true} to intercept messages to children, {@code false} otherwise
     */
    public void intercept(boolean intercept) {
        this.intercept = true;
    }

    /**
     * Checkpoint actor once control has been released.
     * @param flag {@code true} to checkpoint, {@code false} to not checkpoint
     */
    public void checkpoint(boolean flag) {
        if (flag) {
            checkpoint((Serializable & CheckpointRestoreLogic) ctx -> { /* do nothing */ });
        } else {
            checkpoint(null);
        }
    }

    /**
     * Checkpoint actor once control has been released.
     * @param restore restore logic to perform when restoring checkpoint (or {@code null} to not checkpoint)
     */
    public void checkpoint(CheckpointRestoreLogic restore) {
        this.checkpointRestoreLogic = restore;
    }

    /**
     * Checks to see if checkpointing has been set.
     * @return checkpoint restore logic (or {@code null} if not set)
     */
    public CheckpointRestoreLogic checkpoint() {
        return this.checkpointRestoreLogic;
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
     * Get incoming message rule set. Use this to allow/disallow incoming messages of a certain type and/or from a certain address.
     * @return rule set
     */
    public RuleSet ruleSet() {
        return ruleSet;
    }


    
    
    
    
    
    /**
     * Sends a timer request to the timer gateway located at address
     * {@link com.offbynull.actors.core.common.DefaultAddresses#DEFAULT_TIMER_ADDRESS}.
     * @param delay delay in milliseconds
     * @param message message to have the timer reflect back after {@code delay}
     * @throws NullPointerException if any argument is {@code null}
     */
    public void timer(long delay, Object message) {
        out(DEFAULT_TIMER_ADDRESS.appendSuffix(Long.toString(delay)), message);
    }

    /**
     * Sends a error message to the log gateway located at address
     * {@link com.offbynull.actors.core.common.DefaultAddresses#DEFAULT_LOG_ADDRESS}.
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     */
    public void logError(String message, Object... arguments) {
        out(DEFAULT_LOG_ADDRESS, LogMessage.error(message, arguments));
    }

    /**
     * Sends a warn message to the log gateway located at address
     * {@link com.offbynull.actors.core.common.DefaultAddresses#DEFAULT_LOG_ADDRESS}.
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     */
    public void logWarn(String message, Object... arguments) {
        out(DEFAULT_LOG_ADDRESS, LogMessage.warn(message, arguments));
    }
    
    /**
     * Sends a info message to the log gateway located at address
     * {@link com.offbynull.actors.core.common.DefaultAddresses#DEFAULT_LOG_ADDRESS}.
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     */
    public void logInfo(String message, Object... arguments) {
        out(DEFAULT_LOG_ADDRESS, LogMessage.info(message, arguments));
    }
    
    /**
     * Sends a debug message to the log gateway located at address
     * {@link com.offbynull.actors.core.common.DefaultAddresses#DEFAULT_LOG_ADDRESS}.
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     */
    public void logDebug(String message, Object... arguments) {
        out(DEFAULT_LOG_ADDRESS, LogMessage.debug(message, arguments));
    }
    
    /**
     * Sends a trace message to the log gateway located at address
     * {@link com.offbynull.actors.core.common.DefaultAddresses#DEFAULT_LOG_ADDRESS}.
     * @param message message to be logged (SLF4J style)
     * @param arguments arguments to insert in to {@code message}
     * @throws NullPointerException if any argument is {@code null}
     */
    public void logTrace(String message, Object... arguments) {
        out(DEFAULT_LOG_ADDRESS, LogMessage.trace(message, arguments));
    }

    /**
     * Get the context for a child.
     * @param id id of child
     * @return context for child, or {@code null} if no child exists with that id
     * @throws NullPointerException if any argument is {@code null}
     */
    public Context getChildContext(String id) {
        Validate.notNull(id);
        return children.get(id);
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
     * Restore logic to perform.
     */
    public interface CheckpointRestoreLogic {
        /**
         * Perform restore logic.
         * @param ctx actor context
         */
        void perform(Context ctx);
    }

    /**
     * Shortcircuit logic to perform.
     */
    public interface ShortcircuitLogic {
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

    static final class BatchedCreateActorCommand implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String id;
        private final Coroutine actor;
        private final UnmodifiableList<Object> primingMessages;

        BatchedCreateActorCommand(String id, Coroutine actor, Object... primingMessages) {
            Validate.notNull(id);
            Validate.notNull(actor);
            Validate.notNull(primingMessages);
            Validate.noNullElements(primingMessages);
            this.id = id;
            this.actor = actor;
            this.primingMessages = (UnmodifiableList<Object>) unmodifiableList(new ArrayList<>(Arrays.asList(primingMessages)));
        }

        String getId() {
            return id;
        }

        Coroutine getActor() {
            return actor;
        }

        UnmodifiableList<Object> getPrimingMessages() {
            return primingMessages;
        }
    }
    
    static final class BatchedOutgoingMessage implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Address source;
        private final Address destination;
        private final Object message;

        BatchedOutgoingMessage(Address source, Address destination, Object message) {
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
    
    
    
    
    
    
    
    
    
    
    Context parent() {
        return parent;
    }
    
    void parent(Context parent) {
        this.parent = parent;
    }

    CoroutineRunner runner() {
        return runner;
    }
    
    void runner(CoroutineRunner runner) {
        this.runner = runner;
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

    List<BatchedOutgoingMessage> outs() {
        return outs;
    }
    
    Map<String, Context> children() {
        return children;
    }

    boolean intercept() {
        return intercept;
    }

    Map<Class<?>, ShortcircuitLogic> shortcircuits() {
        return shortcircuits;
    }
    
}
