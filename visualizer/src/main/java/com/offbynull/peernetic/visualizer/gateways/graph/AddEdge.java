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
package com.offbynull.peernetic.visualizer.gateways.graph;

import org.apache.commons.lang3.Validate;

/**
 * Message to connect 2 nodes in a graph together.
 * @author Kasra Faghihi
 */
public final class AddEdge {
    private final String fromId;
    private final String toId;

    /**
     * Constructs an {@link AddEdge} instance.
     * @param fromId node id to connect from
     * @param toId node id to connect to
     * @throws NullPointerException if any argument is {@code null}
     */
    public AddEdge(String fromId, String toId) {
        Validate.notNull(fromId);
        Validate.notNull(toId);
        this.fromId = fromId;
        this.toId = toId;
    }

    /**
     * Get node id to connect from.
     * @return node id to connect from
     */
    public String getFromId() {
        return fromId;
    }

    /**
     * Get node id to connect to.
     * @return node id to connect to
     */
    public String getToId() {
        return toId;
    }
    
}
