/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.debug.visualizer;

import java.awt.Color;
import java.awt.Point;
import org.apache.commons.lang3.Validate;

/**
 * Changes a node's scale/location/color.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class ChangeNodeCommand<A> implements Command<A> {
    private A node;
    private Double scale;
    private Point location;
    private Color color;

    /**
     * Constructs a {@link ChangeNodeCommand}.
     * @param node node to change
     * @param scale scale
     * @param location location
     * @param color color
     */
    public ChangeNodeCommand(A node, Double scale, Point location, Color color) {
        Validate.notNull(node); // others can be null
        Validate.isTrue(scale == null || scale >= 0.0, "Negative scale");
        
        this.node = node;
        this.scale = scale;
        this.location = location;
        this.color = color;
    }

    /**
     * Get node.
     * @return node
     */
    public A getNode() {
        return node;
    }

    /**
     * Get scale.
     * @return scale
     */
    public Double getScale() {
        return scale;
    }

    /**
     * Get location (centered).
     * @return location
     */
    public Point getLocation() {
        return location;
    }

    /**
     * Get color.
     * @return color
     */
    public Color getColor() {
        return color;
    }
    
}
