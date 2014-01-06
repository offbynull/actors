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
package com.offbynull.peernetic.actor;

import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.lang3.Validate;

/**
 * Collection of incoming messages.
 * <p/>
 * Used for processing incoming messages for each step of an {@link Actor}.
 * @author Kasra Faghihi
 */
public final class PullQueue {
    private Iterator<Incoming> iterator;

    PullQueue(Collection<Incoming> incoming) {
        Validate.noNullElements(incoming);

        this.iterator = incoming.iterator();
    }

    /**
     * Get the next incoming request.
     * @return next incoming request, or {@code null} if non exists
     */
    public Incoming pull() {
        while (iterator.hasNext()) {
            return iterator.next();
        }
        
        return null;
    }
}
