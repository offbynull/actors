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
package com.offbynull.actors.core.checkpoint;

import com.offbynull.actors.core.context.SourceContext;
import com.offbynull.actors.core.shuttle.Address;

/**
 * Checkpoints and restores actors.
 * <p>
 * The idea behind checkpointing is that an actor gets stored, removed from memory, and then loaded back up again the next time it needs to
 * be interacted with (e.g. a message comes in for it). With checkpointing, a relatively low-power system can service a vast amount of
 * low-activity actors.
 * @author Kasra Faghihi
 */
public interface Checkpointer extends AutoCloseable {
    /**
     * Checkpoint actor.
     * <p>
     * The caller must remove the actor being checkpointed from memory once checkpointing successfully completes. If checkpointing fails for
     * whatever reason, the caller should not remove the actor from memory.
     * @param ctx context to save
     * @return {@code true} if successfully checkpointed, {@code false} if couldn't be checkpointed for whatever reason (e.g. external
     * storage is down)
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code ctx} is not for a root actor
     */
    boolean save(SourceContext ctx);

    /**
     * Restore checkpointed actor.
     * <p>
     * The checkpointed data must be marked as non-loadable in the same transaction that read the data. This needs to be done in order to
     * prevent potential race conditions with restoring and checkpointing actors with the same address.
     * @param address address of actor to restore
     * @return context restored from save, or {@code null} if no such address was checkpointed / if there was a problem accessing checkpoint
     * @throws NullPointerException if any argument is {@code null}
     */
    SourceContext restore(Address address);

    /**
     * Delete checkpointed actor.
     * @param address address of actor to restore
     * @throws NullPointerException if any argument is {@code null}
     */
    void delete(Address address);
}
