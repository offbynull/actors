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

import java.io.Serializable;
import org.apache.commons.lang3.Validate;

/**
 * Message to disconnect to nodes in a graph.
 * @author Kasra Faghihi
 */
public final class RemoveEdge implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String fromId;
    private final String toId;

    /**
     * Constructs an {@link RemoveEdge} instance.
     * @param fromId id of node that edge starts from
     * @param toId to id of node that edge ends at
     * @throws NullPointerException if any argument is {@code null}
     */
    public RemoveEdge(String fromId, String toId) {
        Validate.notNull(fromId);
        Validate.notNull(toId);
        this.fromId = fromId;
        this.toId = toId;
    }

    /**
     * Get id of node that edge starts from.
     * @return from node id
     */
    public String getFromId() {
        return fromId;
    }

    /**
     * Get id of node that edge ends at.
     * @return to node id
     */
    public String getToId() {
        return toId;
    }
    
}
