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
package com.offbynull.peernetic.core.actor;

import com.offbynull.coroutines.user.Coroutine;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.Validate;

/**
 * Builder class for adding an actor.
 * @author Kasra Faghihi
 */
public final class ActorBuilder {
    private static final AtomicInteger ID_GEN = new AtomicInteger();
    
    private Coroutine actor;
    private String id = Integer.toString(ID_GEN.getAndIncrement());
    private List<Object> primingMessages = new LinkedList<>();

    /**
     * Set actor.
     * @param actor actor
     * @return this builder
     */
    public ActorBuilder actor(Coroutine actor) {
        this.actor = actor;
        return this;
    }
    
    /**
     * Set actor ID (not required -- if missing one is generated).
     * @param id id to use
     * @return this builder
     */
    public ActorBuilder id(String id) {
        this.id = id;
        return this;
    }
    
    /**
     * Add priming messages.
     * @param msgs messages to add
     * @return this builder
     */
    public ActorBuilder prime(Object ... msgs) {
        primingMessages.addAll(Arrays.asList(msgs));
        return this;
    }

    /**
     * Adds the actor to a {@link ActorRunner}.
     * @param instance runner to add to
     * @see ActorRunner#addActor(java.lang.String, com.offbynull.coroutines.user.Coroutine, java.lang.Object...) 
     */
    public void invoke(ActorRunner instance) {
        Validate.notNull(instance);
        instance.addActor(id, actor, primingMessages.toArray());
    }

    /**
     * Adds the actor (as a child actor) to a {@link SourceContext}.
     * @param instance runner to add to
     * @see SourceContext#child(java.lang.String, com.offbynull.coroutines.user.Coroutine, java.lang.Object[]) 
     */
    public void invoke(SourceContext instance) {
        Validate.notNull(instance);
        instance.child(id, actor, primingMessages);
    }
}
