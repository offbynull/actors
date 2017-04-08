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
package com.offbynull.actors.core.cache;

import com.offbynull.actors.core.context.SourceContext;
import com.offbynull.actors.core.shuttle.Address;

/**
 * Caches and restores actors.
 * <p>
 * The idea behind caching is that an actor gets stored, removed from memory, and then loaded back up again the next time it needs to be
 * interacted with (e.g. a message comes in for it). With caching, a relatively low-power system can service a vast amount of low-activity
 * actors.
 * @author Kasra Faghihi
 */
public interface Cacher extends AutoCloseable {
    /**
     * Cache actor.
     * <p>
     * The caller must be removed from actor being cached from memory once caching successfully completes. If caching fails for whatever
     * reason, the caller should not remove the actor from memory.
     * @param ctx context to save
     * @return {@code true} if successfully cached, {@code false} if couldn't be cached for whatever reason (e.g. external storage is down)
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code ctx} is not for a root actor
     */
    boolean save(SourceContext ctx);

    /**
     * Restore cached actor.
     * <p>
     * The cached data must be marked as non-loadable in the same transaction that read the data. This needs to be done in order to prevent
     * potential race conditions with restoring and caching actors with the same address.
     * @param address address of actor to restore
     * @return context restored from save, or {@code null} if no such address was cached
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if problem accessing storage
     */
    SourceContext restore(Address address);

    /**
     * Delete cached actor.
     * <p>
     * The cached data must be deleted in the same transaction that read the data. This needs to be done in order to prevent potential race
     * conditions with restoring and caching actors with the same address.
     * @param address address of actor to restore
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if problem accessing storage
     */
    void delete(Address address);
}
