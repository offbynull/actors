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
 * Message to apply a JavaFX CSS effect to an edge.
 * @author Kasra Faghihi
 */
public final class StyleEdge implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String fromId;
    private final String toId;
    private final int color;
    private final double width;

    /**
     * Constructs a {link StyleEdge} instance.
     * @param fromId id of node that edge starts from
     * @param toId to id of node that edge ends at
     * @param color 24-bit RGB color value to apply to edge (top 8-bits, usually used as alpha, must be 0)
     * @param width width to apply to the edge
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if top 8-bits of {@code color} are not {@code 0}, or if {@code width <= 0}
     */
    public StyleEdge(String fromId, String toId, int color, double width) {
        Validate.notNull(fromId);
        Validate.notNull(toId);
        Validate.isTrue((color & 0xFF000000) == 0);
        Validate.isTrue(width > 0.0);
        
        this.fromId = fromId;
        this.toId = toId;
        this.color = color;
        this.width = width;
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

    /**
     * Get 24-bit color to apply to edge.
     * @return color to apply
     */
    public int getColor() {
        return color;
    }

    /**
     * Get width to apply to edge.
     * @return width to apply
     */
    public double getWidth() {
        return width;
    }

    @Override
    public String toString() {
        return "StyleEdge{" + "fromId=" + fromId + ", toId=" + toId + ", color=" + color + ", width=" + width + '}';
    }

}
