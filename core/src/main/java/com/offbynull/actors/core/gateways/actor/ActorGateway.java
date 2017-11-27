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
package com.offbynull.actors.core.gateways.actor;

import static com.offbynull.actors.core.gateway.CommonAddresses.DEFAULT_ACTOR;
import com.offbynull.actors.core.gateway.Gateway;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.actors.core.shuttle.Address;
import com.offbynull.actors.core.shuttle.Shuttle;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.offbynull.actors.core.stores.memory.MemoryStore;
import com.offbynull.actors.core.shuttle.Message;
import com.offbynull.coroutines.user.CoroutineRunner;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import com.offbynull.actors.core.store.Store;

/**
 * Gateway that executes distributed actors.
 * <p>
 * The following usage example creates an instance of {@link ActorGateway}, adds an actor to it, and then shuts it down.
 * <pre>
 * // Create an ActorGateway. The address of all actors added will be prefixed with "local"
 * ActorGateway actorGateway = new ActorGateway("local");
 * 
 * // Add a new coroutine actor with the address "local:actor2". As soon as the actor is added, it will receive 2 incoming messages from
 * // itself: "start1" and "start2".
 * Coroutine myCoroutineActor = ...;
 * actorGateway.addActor("actor2", myCoroutineActor, "start1", "start2");
 * 
 * 
 * ... do some unrelated stuff here ...
 * 
 * 
 * 
 * // Shutdown the ActorGateway
 * actorGateway.close();
 * </pre>
 * 
 * All actors assigned to a runner can send messages to each other without any additional setup.
 * <p>
 * If you want outside components to be able to send messages to actors assigned to a runner, you'll need to pass those
 * outside components a reference to the {@link Shuttle} returned by {@link #getIncomingShuttle() }.
 * <p>
 * Similarly, if actors assigned to a runner are going to send messages to outside components, the {@link Shuttle}s for
 * those outgoing components need to be added using {@link #addOutgoingShuttle(com.offbynull.actors.core.shuttle.Shuttle) }.
 * <p>
 * If an actor tries to send a message to an address for which no outgoing shuttle has been added, that message is silently discarded.
 * 
 * @author Kasra Faghihi
 */
public final class ActorGateway implements Gateway, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ActorGateway.class);
    
    private final AtomicBoolean shutdownFlag;
    private final Thread[] threads;
    
    private final String prefix;
    private final ActorShuttle selfShuttle;
    private final ConcurrentHashMap<String, Shuttle> outShuttles;
    private final Store store;

    /**
     * Create a {@link ActorGateway} instance. Equivalent to calling {@code create(DefaultAddresses.DEFAULT_ACTOR)}.
     * @return new direct gateway
     */
    public static ActorGateway create() {
        return create(DEFAULT_ACTOR);
    }

    /**
     * Create an {@link ActorGateway} instance. with the number of threads set to the number of processors available on the system.
     * Equivalent to calling {@code ActorGateway.create(prefix, Runtime.getRuntime().availableProcessors())}.
     * @param prefix address prefix to use for actors that get added to this runner
     * @throws NullPointerException if any argument is {@code null}
     * @return new actor runner
     */
    public static ActorGateway create(String prefix) {
        return ActorGateway.create(prefix, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Create an {@link ActorGateway} instance. Equivalent to calling
     * {@code ActorGateway.create(prefix, threadCount, new MemoryStore(prefix, threadCount))}.
     * @param prefix address prefix to use for actors that get added to this runner
     * @param threadCount number of threads to use for this runner
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code threadCount < 1}
     * @return new actor runner
     */
    public static ActorGateway create(String prefix, int threadCount) {
        return ActorGateway.create(prefix, threadCount, new MemoryStore(prefix, threadCount));
    }

    /**
     * Create an {@link ActorGateway} instance.
     * @param prefix address prefix to use for actors that get added to this runner
     * @param threadCount number of threads to use for this runner
     * @param store storage engine
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code threadCount < 1}
     * @return new actor runner
     */
    public static ActorGateway create(String prefix, int threadCount, Store store) {
        Validate.notNull(prefix);
        Validate.notNull(store);
        Validate.isTrue(threadCount > 0);

        ActorGateway ret = new ActorGateway(prefix, threadCount, store);
        
        // Handler to call if any of the threads encounter a problem while they're running. If any thread encounters a critical error, then
        // all threads must be shut down!
        FailListener failListener = (t) -> {
            LOG.error("Critical failure handler invoked! Signalling all threads to close.", t);
            ret.shutdownFlag.set(true);
        };
        
        // Create and start threads
        try {
            for (int i = 0; i < threadCount; i++) {
                ActorRunnable actorRunnable = new ActorRunnable(ret.prefix, ret.outShuttles, ret.store, failListener, ret.shutdownFlag);
                ret.threads[i] = new Thread(actorRunnable);
                ret.threads[i].start();
            }
        } catch (RuntimeException e) {
            ret.shutdownFlag.set(true);
            throw e;
        }
        
        return ret;
    }
    
    private ActorGateway(String prefix, int threadCount, Store store) {
        Validate.notNull(prefix);
        Validate.notNull(store);
        Validate.isTrue(threadCount > 0);
        
        this.prefix = prefix;
        this.threads = new Thread[threadCount];
        this.shutdownFlag = new AtomicBoolean(false);
        this.selfShuttle = new ActorShuttle(prefix, store, shutdownFlag);
        this.outShuttles = new ConcurrentHashMap<>();
        this.store = store;
    }
    
    @Override
    public Shuttle getIncomingShuttle() {
        if (shutdownFlag.get()) {
            throw new IllegalStateException();
        }

        return selfShuttle;
    }

    /**
     * Add an actor to the system.
     * <p>
     * If this runner has been shutdown prior to calling this method, this method does nothing.
     * @param id id to use for actor. For example, if the prefix for this runner is "runner", and the id of the actor being add is "test",
     * that actor will be accessible via the address "runner:test".
     * @param coroutine coroutine being added
     * @param primingMessages messages to send to {@code actor} (shown as coming from itself) once its been added
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalStateException if this gateway is closed
     */
    public void addActor(String id, Coroutine coroutine, Object... primingMessages) {
        Validate.notNull(id);
        Validate.notNull(coroutine);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        if (shutdownFlag.get()) {
            throw new IllegalStateException();
        }

        Address self = Address.of(prefix, id);
        Context ctx = new Context(self);

        CoroutineRunner runner = new CoroutineRunner(coroutine);
        runner.setContext(ctx);
        
        Actor actor = new Actor(null, runner, ctx);
        SerializableActor serializableActor = SerializableActor.serialize(actor);

        Message[] messages = Arrays.stream(primingMessages)
                .map(payload -> new Message(ctx.self(), ctx.self(), payload))
                .toArray(size -> new Message[size]);

        store.store(serializableActor);
        store.store(messages);
    }

    @Override
    public void addOutgoingShuttle(Shuttle shuttle) {
        Validate.notNull(shuttle);
        if (shutdownFlag.get()) {
            throw new IllegalStateException();
        }
        
        String shuttlePrefix = shuttle.getPrefix();
        outShuttles.put(shuttlePrefix, shuttle);
    }

    @Override
    public void removeOutgoingShuttle(String shuttlePrefix) {
        Validate.notNull(shuttlePrefix);
        if (shutdownFlag.get()) {
            throw new IllegalStateException();
        }

        outShuttles.remove(shuttlePrefix);
    }
    
    @Override
    public void close() {
        shutdownFlag.set(true);
    }

    @Override
    public void join() throws InterruptedException {
        for (Thread thread : threads) {
            thread.join();
        }
    }
}
