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
package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Actor;
import com.offbynull.peernetic.core.shuttle.Address;

/**
 * Interface for a child coroutine, intended to be run as part of an {@link Actor}.
 * <p>
 * @see SubcoroutineRouter
 * @author Kasra Faghihi
 * @param <T> result type
 */
public interface Subcoroutine<T> {
    /**
     * Get the id of this subcoroutine. Incoming messages destined for this id should trigger this subcoroutine to run. The id returned must
     * be an absolute id, not a relative id. Meaning that if there's a hierarchy, each invocation in the chain one should return the full
     * id, not the id relative to its parent.
     * @return id
     */
    Address getId();
    /**
     * Entry point of subcoroutine.
     * @param cnt used to suspend/yield this subcoroutine
     * @return result of this subcoroutine
     * @throws Exception if a problem occurs
     */
    T run(Continuation cnt) throws Exception;
}
