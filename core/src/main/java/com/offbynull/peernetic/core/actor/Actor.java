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
 * Interface for an actor. There's a good deal of information available on the theoretical concept of actors and their role in
 * concurrent/distributed computing on <a href="http://en.wikipedia.org/wiki/Actor_model">Wikipedia's Actor page</a>. In terms of
 * Peernetic's implementation of actors, implementations of this interface should ...
 * 
 * <ol>
 * <li><b>not expose any of their own internal state to the outside.</b> Meaning that implementations should not have any setters, getters,
 * public fields, or any other direct mechanism for exposing their state. If another actor or outside component wants to know or change the
 * internal state of an actor, it should request it by sending that actor a message.</li>
 * <li><b>only ever directly access/change their own internal state.</b> Meaning that implementations should not share any references with
 * other actors or outside objects, unless those references are to immutable objects. For example, an actor shouldn't have a reference to a
 * ConcurrentHashMap that's being shared with other actors. Communication between actors or other outside components should be done via
 * message-passing.</li>
 * <li><b>avoid blocking, whether it's blocking from I/O, a long running operation, thread synchronization, or otherwise.</b> It's possible
 * for multiple actors to be running in the same Java thread. That means that if an actor blocks for any reason, it may deprive other actors
 * of processing their own messages in a timely manner. In addition to that, directly doing I/O may cause complications should you ever try
 * to serialize an actor.</li>
 * <li><b>only ever communicates with the outside world (e.g. other actors and outside objects) through asynchronous message-passing.</b>
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
     * Called when an actor receives a new message. Each time this method is invoked, a {@code context} is supplied. That context
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
