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

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.actor.ActorRunnable.AddShuttleMessage;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import org.apache.commons.lang3.Validate;

/**
 * Container used to execute {@link Actor}s.
 * <p>
 * The following usage example creates an instance of {@link ActorThread}, adds a normal {@link Actor} and a coroutine-based {@link Actor}
 * to it, and then shuts it down.
 * <pre>
 * // Create an ActorThread. The address of all actors added will be prefixed with "local"
 * ActorThread actorThread = ActorThread.create("local");
 * 
 * // Add a new actor with the address "local:actor1". As soon as the actor is added, it will receive an incoming message from itself that
 * // is the string "start".
 * Actor myActor = ...;
 * actorThread.addActor("actor1", myActor, "start");
 * 
 * // Add a new coroutine actor with the address "local:actor2". As soon as the actor is added, it will receive 2 incoming messages from
 * // itself: "start1" and "start2".
 * Coroutine myCoroutineActor = ...;
 * actorThread.addCoroutineActor("actor2", myCoroutineActor, "start1", "start2");
 * 
 * 
 * ... do some unrelated stuff here ...
 * 
 * 
 * 
 * // Shutdown the ActorThread
 * actorThread.shutdown();
 * </pre>
 * 
 * All actors assigned to this {@link ActorThread} can send messages to each other without any additional setup.
 * <p>
 * If you want outside components (e.g. other actors that are assigned to a different {@link ActorThread}s) to be able to send messages to
 * actors assigned to this {@link ActorThread}, you'll need to pass those outside components a reference to the {@link Shuttle} returned by
 * {@link #getIncomingShuttle() }.
 * <p>
 * Similarly, if actors assigned to this {@link ActorThread} are going to send messages to outside components, the {@link Shuttle}s for
 * those outgoing components need to be added to this {@link ActorThread} using
 * {@link #addOutgoingShuttle(com.offbynull.peernetic.core.shuttle.Shuttle) }.
 * 
 * If an actor tries to send a message to an address for which no outgoing shuttle has been added, that message is silently discarded.
 * 
 * @author Kasra Faghihi
 */
public final class ActorThread implements AutoCloseable {
    private final Thread thread;
    private final ActorRunnable actorRunnable;
    private final Bus bus;

    ActorThread(Thread thread, Bus bus, ActorRunnable actorRunnable) {
        Validate.notNull(thread);
        Validate.notNull(bus);
        Validate.notNull(actorRunnable);
        this.thread = thread;
        this.bus = bus;
        this.actorRunnable = actorRunnable;
    }
    
    /**
     * Creates a new {@link ActorThread} object.
     * @param prefix address prefix to use for actors that get added to this {@link ActorThread}
     * @return newly created {@link ActorThread}
     * @throws NullPointerException if any argument is {@code null}
     */
    public static ActorThread create(String prefix) {
        Validate.notNull(prefix);
        
        // create runnable
        Bus bus = new Bus();
        ActorRunnable runnable = new ActorRunnable(prefix, bus);
        
        Shuttle selfShuttle = runnable.getIncomingShuttle();

        // add in our own shuttle as well so we can send msgs to ourselves
        bus.add(new AddShuttleMessage(selfShuttle));

        // start thread
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName(ActorRunnable.class.getSimpleName());
        thread.start();

        // return
        return new ActorThread(thread, bus, runnable);
    }
    
    /**
     * Shuts down this {@link ActorThread}. Blocks until the internal thread that executes actors terminates before returning.
     * @throws InterruptedException if interrupted while waiting for shutdown
     */
    @Override
    public void close() throws InterruptedException {
        try {
            bus.close();
        } catch (Exception e) {
            // do nothing
        }
        
        thread.interrupt();
        thread.join();
    }

    /**
     * Blocks until the internal thread that executes actors terminates.
     * @throws InterruptedException if interrupted while waiting
     */
    public void join() throws InterruptedException {
        thread.join();
    }
    
    /**
     * Get the shuttle used to receive messages from outside components.
     * @return shuttle for incoming messages to this {@link ActorThread}
     */
    public Shuttle getIncomingShuttle() {
        return actorRunnable.getIncomingShuttle();
    }

    /**
     * Queue an actor to be added. Note that this method queues an actor to be added rather than adding it right away. As such, this
     * method will likely return before the actor in question is added and any error during encountered during adding will not be
     * known to the caller. For example, if you try to use an id that has already been added, the exception won't be relayed to the caller.
     * <p>
     * If this {@link ActorThread} has been shutdown prior to calling this method, this method does nothing.
     * @param id id to use for {@code actor}
     * @param actor actor being added
     * @param primingMessages messages to send to this actor (shown as coming from itself) once the actor's been added
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public void addActor(String id, Actor actor, Object... primingMessages) {
        Validate.notNull(id);
        Validate.notNull(actor);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        actorRunnable.addActor(id, actor, primingMessages);
    }

    /**
     * Queue a coroutine-based actor to be added. Equivalent to calling
     * {@code addActor(id, new CoroutineActor(coroutine), primingMessages)}.
     * <p>
     * If this {@link ActorThread} has been shutdown prior to calling this method, this method does nothing.
     * @param id id to use for actor being added
     * @param coroutine coroutine for actor being added
     * @param primingMessages messages to send to this actor (shown as coming from itself) once the actor's been added
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public void addCoroutineActor(String id, Coroutine coroutine, Object... primingMessages) {
        Validate.notNull(id);
        Validate.notNull(coroutine);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        actorRunnable.addCoroutineActor(id, coroutine, primingMessages);
    }

    /**
     * Queue an actor to be remove. Note that this method queues an actor to be removed rather than removing it right away. As such, this
     * method will likely return before the actor in question is removed and any error during encountered during removal will not be
     * known to the caller.
     * <p>
     * If this {@link ActorThread} has been shutdown prior to calling this method, this method does nothing.
     * <p>
     * If this {@link ActorThread} doesn't contain an actor with the id {@code id}, nothing will be removed.
     * @param id id of actor to remove
     * @throws NullPointerException if any argument is {@code null}
     */
    public void removeActor(String id) {
        Validate.notNull(id);
        actorRunnable.removeActor(id);
    }

    /**
     * Queue an outgoing shuttle to be added. When an actor sends a message, that message will be forwarded to the appropriate outgoing
     * shuttle (based on the prefix of the destination address). If no outgoing shuttle is found, the message is silently discarded.
     * <p>
     * Note that this operation queues a shuttle to be added rather than adding it right away. As such, this method will likely
     * return before the add operation completes and any error encountered during the operation will not be known to the caller.
     * <p>
     * If this {@link ActorThread} has been shutdown prior to calling this method, this method does nothing.
     * <p>
     * @param shuttle outgoing shuttle to add
     * @throws NullPointerException if any argument is {@code null}
     */
    public void addOutgoingShuttle(Shuttle shuttle) {
        Validate.notNull(shuttle);
        actorRunnable.addOutgoingShuttle(shuttle);
    }

    /**
     * Queue an outgoing shuttle for removal.
     * <p>
     * Note that this operation queues a shuttle to be added rather than adding it right away. As such, this method will likely
     * return before the add operation completes and any error encountered during the operation will not be known to the caller.
     * <p>
     * If this {@link ActorThread} has been shutdown prior to calling this method, this method does nothing.
     * <p>
     * @param prefix address prefix for shuttle to remove
     * @throws NullPointerException if any argument is {@code null}
     */
    public void removeOutgoingShuttle(String prefix) {
        Validate.notNull(prefix);
        actorRunnable.removeOutgoingShuttle(prefix);
    }
    
}
