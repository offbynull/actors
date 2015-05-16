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
 * Message to move a node in a graph.
 * @author Kasra Faghihi
 */
public final class MoveNode {
    private final String id;
    private final double x;
    private final double y;

    /**
     * Constructs a {@link MoveNode} instance.
     * @param id node id to move
     * @param x x coordinate to move node to
     * @param y y coordinate to move node to
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any double argument is not finite
     */
    public MoveNode(String id, double x, double y) {
        Validate.notNull(id);
        Validate.isTrue(Double.isFinite(x));
        Validate.isTrue(Double.isFinite(y));
        this.id = id;
        this.x = x;
        this.y = y;
    }

    /**
     * Get id of node being moved.
     * @return node id
     */
    public String getId() {
        return id;
    }

    /**
     * Get x coordinate to move node to.
     * @return x coordinate
     */
    public double getX() {
        return x;
    }

    /**
     * Get y coordinate to move node to.
     * @return y coordinate
     */
    public double getY() {
        return y;
    }
    
}
