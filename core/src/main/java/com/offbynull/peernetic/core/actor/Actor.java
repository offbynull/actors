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

/**
 * An actor is an isolated "computational unit" who's only method of communicating with the outside world (other actors or components) is
 * through message-passing. If you aren't familiar with concept of actors and their role in concurrent/distributed computing, there's a good
 * introduction available on <a href="http://en.wikipedia.org/wiki/Actor_model">Wikipedia's Actor page</a>.
 * 
 * Implementations of this interface should adhere to the following constraints:
 * 
 * <ol>
 * <li><b>Do not expose any internal state.</b> Unlike traditional objects that communicate by invoking methods on each other,
 * actors should not provide any setters, getters, public fields, or any other direct mechanism for exposing their state. If another actor
 * or outside component needs to know or change the internal state of this actor, it must request it via a message.</li>
 * <li><b>Do not share state.</b> Only ever directly access/change your own internal state. An actor should not share any references with
 * other actors or outside objects, unless those references are to immutable objects. For example, an actor shouldn't have a reference to a
 * ConcurrentHashMap that's being shared with other actors or components. Communication between actors or other outside components must be
 * done via message-passing.</li>
 * <li><b>Avoid blocking, whether it's for I/O, a long running operation, thread synchronization, or otherwise.</b> Multiple actors may be
 * running in the same Java thread, therefore, if an actor were to block for any reason, it may prevent other actors from processing
 * messages in a timely manner. In addition, any actors that directly perform I/O may be incapable of being serialized.
 * </li>
 * <li><b>Only ever communicate with the outside world (e.g. other actors and components) through asynchronous message-passing.</b>
 * Since implementations avoid sharing and exposing state, there needs be some mechanism to communicate interface with the outside.
 * Message-passing is that mechanism.</li>
 * </ol>
 * 
 * Following the above implementation rules means that, outside of receiving messages and sending messages, an actor is fully isolated. This
 * isolation helps with concurrency (no shared state, so we don't have to worry about synchronizing state) and transparency (it doesn't
 * matter if you're passing messages to a component that's remote or local, the underlying message-passing should make it transparent).
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
     * <li>serializable -- can be written out to a byte stream and read back in.</li>
     * <li>deterministic -- serializing a message then deserializing it must always result in the same object.</li>
     * </ol>
     * @param context context for this actor. The same {@link Context} object is passed in to this method on every invocation for the entire
     * life of the actor.
     * @return {@code true} if the actor hasn't finished, {@code false} if it has finished
     * @throws Exception when any unhandled error occurs -- if an exception is thrown it means that this actor has had an unrecoverable
     * error and should stop executing
     */
    boolean onStep(Context context) throws Exception;
}
