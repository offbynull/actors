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

/**
 * An {@link Actor} is an isolated "computational unit" whose only method of communicating with the outside world is through
 * message-passing. If you aren't familiar with the concept of actors and their role in concurrent/distributed computing, there's a good
 * introduction available on <a href="http://en.wikipedia.org/wiki/Actor_model">Wikipedia's Actor page</a>.
 * <p>
 * Implementations of this interface must adhere to the following constraints:
 * <ul>
 * <li><b>Do not expose any internal state.</b> Unlike traditional Java objects, actors should not provide any publicly accessibly methods
 * or fields that expose or change their state. If an outside component needs to know or change the state of an actor, it must request it
 * via message-passing.</li>
 * <li><b>Do not share state.</b> Actors must only ever access/change their own internal state, meaning that an actor must not share any
 * references with other outside objects (unless those references are to immutable objects). For example, an actor shouldn't have a
 * reference to a ConcurrentHashMap that's being shared with other objects. As stated in the previous constraint, communication must be done
 * via message-passing.</li>
 * <li><b>Do not block</b> for I/O, long running operations, thread synchronization, or anything else. Multiple actors may be
 * running in the same Java thread. As such, if an actor were to block for any reason, it may prevent other actors from processing messages
 * in a timely manner.</li>
 * <li><b>Do not directly access time.</b> Actors must use the time supplied to them via {@link Context#getTime() } rather than making
 * calls to Java's date and time APIs (e.g. {@link Instant} or {@link System#currentTimeMillis() }).</li>
 * </ul>
 * <p>
 * Following the above implementation rules means that, outside of receiving and sending messages, an actor is fully isolated. This
 * isolation helps with concurrency (no shared state, so we don't have to worry about synchronizing state) and transparency (it doesn't
 * matter if you're passing messages to a component that's remote or local, the underlying transport mechanism should be transparent).
 * 
 * @author Kasra Faghihi
 */
public interface Actor {
    /**
     * Called when this actor receives a new message. Each time this method is invoked, a {@link Context} is supplied. That context
     * contains ...
     * <ul>
     * <li>the address of this actor.</li>
     * <li>the incoming message that triggered the invocation of this method.</li>
     * <li>the address the incoming message was sent from.</li>
     * <li>the address the incoming message was sent to.</li>
     * <li>a queue of outgoing messages (add messages to this queue and once this method returns those messages will get sent).</li>
     * <li>the current time (always use this if you need the current time as opposed to directly accessing the time via Java's APIs like
     * {@code Instant.now()} or {@code System.getCurrentTimeMillis()}).
     * </ul>
     * <p>
     * Remember that both incoming and outgoing messages must be ...
     * <ol>
     * <li>immutable -- cannot change once created.</li>
     * <li>serializable -- can be written out to a stream and read back in.</li>
     * <li>deterministic -- serializing a message and then deserializing it must always result in the same object.</li>
     * </ol>
     * @param context context for this actor. The same {@link Context} object is passed in to this method on every invocation for the entire
     * life of the actor.
     * @return {@code true} if the actor hasn't finished, {@code false} if it has finished
     * @throws Exception when any unhandled error occurs -- if an exception is thrown it means that this actor has had an unrecoverable
     * error and should stop executing
     */
    boolean onStep(Context context) throws Exception;
}
