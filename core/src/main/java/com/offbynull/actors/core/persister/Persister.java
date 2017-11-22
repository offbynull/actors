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
package com.offbynull.actors.core.persister;

import com.offbynull.actors.core.gateways.actor.SerializableActor;
import com.offbynull.actors.core.shuttle.Address;
import com.offbynull.actors.core.shuttle.Message;
import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;

/**
 * Actor persister.
 * <p>
 * Implementations must be robust. It should only throw exceptions for critical errors. For example, if the implementation encounters
 * connectivity issues, rather than throwing an exception it should block and retry until the issue has been resolved.
 * @author Kasra Faghihi
 */
public interface Persister extends Closeable {

    /**
     * Puts actor into storage, replacing it if it already exists.
     * @param actor actor to persist
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if the operation could not complete successfully, or if this persister has been closed
     */
    void store(SerializableActor actor);

    /**
     * Equivalent to calling {@code store(Arrays.asList(messages))}.
     * @param messages messages coming from {@code actor}
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalStateException if the operation could not complete successfully, or if this persister has been closed
     */
    default void store(Message... messages) {
        store(Arrays.asList(messages));
    }
    
    /**
     * Puts messages coming in for an actor into storage. If the actor for an incoming message isn't already in storage, the message may
     * be silently discarded (depends on the implementation).
     * @param messages messages coming from {@code actor}
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalStateException if the operation could not complete successfully, or if this persister has been closed
     */
    void store(Collection<Message> messages);
    
    /**
     * Equivalent to calling {@code discard(Address.fromString(address))}.
     * @param address address of actor to discard
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalStateException if the operation could not complete successfully, or if this persister has been closed
     */
    default void discard(String address) {
        discard(Address.fromString(address));
    }
    
    /**
     * Removes/discards an actor from storage. Does nothing if the actor being removed doesn't exist
     * @param address address of actor to discard
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalStateException if the operation could not complete successfully, or if this persister has been closed
     */
    void discard(Address address);

    /**
     * Take a piece of work (message along with the actor responsible for processing it) out of storage. If no work is available, this
     * method blocks until work becomes available.
     * @return actor and message for actor
     * @throws IllegalStateException if the operation could not complete successfully, or if this persister has been closed
     */
    PersisterWork take();
}
