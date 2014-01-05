/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.common.concurrent.actor;

import java.util.Collection;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.lang3.Validate;

/**
 * Use to test an {@link Actor}.
 * @author Kasra Faghihi
 */
public final class ActorTester {
    private Actor actor;
    
    /**
     * Construct an {@link ActorTester} object. 
     * @param actor actor to test -- once passed in, the actor will be unusable
     * @throws IllegalStateException if actor already started or stopped.
     * @throws NullPointerException if any argument is {@code null}
     */
    public ActorTester(Actor actor) {
        Validate.notNull(actor);
        
        actor.readyForTesting();
        this.actor = actor;
    }

    /**
     * Call the wrapped actor's {@link Actor#onStart() } method.
     * @throws Exception on error
     */
    public void testOnStart() throws Exception {
        actor.testOnStart();
    }

    /**
     * Call the wrapped actor's {@link Actor#onStep(long, java.util.Iterator, com.offbynull.peernetic.common.concurrent.actor.PushQueue) }
     * method.
     * @param messages messages to pass in
     * @param timestamp timestamp to pass in
     * @return outgoing messages
     * @throws Exception on error
     */
    public MultiMap<ActorQueueWriter, Message> testOnStep(long timestamp, Collection<Message> messages) throws Exception {
        return actor.testOnStep(timestamp, messages).get();
    }

    /**
     * Call the wrapped actor's {@link Actor#onStop(com.offbynull.peernetic.common.concurrent.actor.PushQueue)  } method.
     * @return outgoing messages
     * @throws Exception on error
     */
    public MultiMap<ActorQueueWriter, Message> testOnStop() throws Exception {
        return actor.testOnStop().get();
    }
    
    
}
