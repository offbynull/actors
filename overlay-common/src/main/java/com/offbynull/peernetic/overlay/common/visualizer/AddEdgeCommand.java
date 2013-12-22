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
package com.offbynull.peernetic.overlay.common.visualizer;

import org.apache.commons.lang3.Validate;

/**
 * Command to add an edge.
 * @author Kasra F
 * @param <A> address type
 */
public final class AddEdgeCommand<A> implements Command<A> {
    private A from;
    private A to;

    /**
     * Constructs an {@link AddEdgeCommand}.
     * @param from edge source node
     * @param to edge destination node
     * @throws NullPointerException if any arguments are {@code null}
     */
    public AddEdgeCommand(A from, A to) {
        Validate.notNull(from);
        Validate.notNull(to);
        
        this.from = from;
        this.to = to;
    }

    /**
     * Get source.
     * @return source
     */
    public A getFrom() {
        return from;
    }

    /**
     * Get destination.
     * @return destination
     */
    public A getTo() {
        return to;
    }
    
}
