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
 * An actor is an object that only ever accesses/changes its own internal state, doesn't expose any of it's internal state to the outside
 * world, and only ever communicates with the outside world (e.g. other actors and outside objects) through asynchronous message-passing.
 * In other words, outside of receiving messages and sending messages, actors must be fully isolated. This isolation helps with concurrency
 * (no shared state, so we don't have to worry about synchronizing state) and transparency (it doesn't matter if an actor is remote or
 * local).
 * 
 * Implementations of this interface ...
 * 
 * <ol>
 * <li>
 * must not have any setters, getters, public fields, or any other direct mechanism for exposing it's state. If another actor or outside
 * component wants to know or change the internal state of an actor, it must request it by sending that actor a message.
 * </li>
 * <li>
 * must not share any references with other actors or outside objects, unless those references are to immutable objects. For example, an 
 * actor shouldn't have a reference to a ConcurrentHashMap that's being shared with other actors. Communication between actors or other
 * outside components must be done via message passing.
 * </li>
 * <li>
 * must not perform any threading-related operations (e.g. synchronizing or atomic variables). It wouldn't make sense to perform any
 * threading-related operations given that an actor isn't sharing state (see rules above).
 * </li>
 * <li>
 * must not perform any I/O (e.g. reading/writing a file). I/O in particularly should be done by passing a message to some other component
 * that performs the actual I/O operation(s). It's possible for multiple actors to be running in the same Java thread. That means that if an
 * actor blocks while it tries to perform one or more I/O operations, it may deprive other actors of processing their own messages in a
 * timely manner. In addition to that, directly doing I/O may cause complications should you ever try to serialize an actor.
 * </li>
 * <li>
 * must not perform long running operations. The reasoning is the same as the reasoning for not doing I/O in the actor... It's possible
 * for multiple actors to be running in the same Java thread. That means that if an actor spends a long time doing some calculation, it may
 * deprive other actors of processing their own messages in a timely manner.
 * </li>
 * </ol>
 * 
 * There's a good deal of information available on the theoretical concept of actors and their role in concurrent/distributed computing on
 * <a href="http://en.wikipedia.org/wiki/Actor_model">Wikipedia's Actor page</a>. The general idea is to abstract 
 * 
 * @author Kasra Faghihi
 */
public interface Actor {
    DOCUMENTME;
    boolean onStep(Context context) throws Exception;
}
