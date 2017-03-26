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
 * Message to add a node to a graph.
 * @author Kasra Faghihi
 */
public final class AddNode implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String id;

    /**
     * Constructs an {@link AddNode} instance.
     * @param id id of node being added
     * @throws NullPointerException if any argument is {@code null}
     */
    public AddNode(String id) {
        Validate.notNull(id);
        this.id = id;
    }

    /**
     * Get id of node to be added.
     * @return node id
     */
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "AddNode{" + "id=" + id + '}';
    }
    
}