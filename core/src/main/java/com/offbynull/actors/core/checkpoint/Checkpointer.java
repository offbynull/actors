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
import java.util.concurrent.Future;

/**
 * Saves and restores actors.
 * @author Kasra Faghihi
 */
public interface Checkpointer extends AutoCloseable {

    /**
     * Save actor.
     * @param ctx context to save
     * @return a future that completes once the actor has saved
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code ctx} is not for a root actor
     */
    Future<Void> checkpoint(SourceContext ctx);
    
    /**
     * Restore actors. Note that closing this checkpointer won't necessarily also close the iterators returned by this method.
     * @return iterator that returns checkpointed actors
     * @throws IllegalStateException if problem accessing storage
     */
    RestoreResultIterator restore();
}
