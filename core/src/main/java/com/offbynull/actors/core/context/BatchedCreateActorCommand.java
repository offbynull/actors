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
package com.offbynull.actors.core.context;

import com.offbynull.coroutines.user.Coroutine;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * A queued command to spawn a root actor.
 * @author Kasra Faghihi
 */
public final class BatchedCreateActorCommand implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private final String id;
    private final Coroutine actor;
    private final List<Object> primingMessages;

    /**
     * Constructs a {@link BatchedCreateActorCommand}.
     * @param id id of actor
     * @param actor actor
     * @param primingMessages messages to prime actor
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public BatchedCreateActorCommand(String id, Coroutine actor, Object ... primingMessages) {
        Validate.notNull(id);
        Validate.notNull(actor);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        this.id = id;
        this.actor = actor;
        this.primingMessages = new ArrayList<>(Arrays.asList(primingMessages));
    }

    /**
     * Get id.
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * Get actor.
     * @return actor
     */
    public Coroutine getActor() {
        return actor;
    }

    /**
     * Get priming messages.
     * @return priming messages
     */
    public List<Object> getPrimingMessages() {
        return new ArrayList<>(primingMessages);
    }
}
