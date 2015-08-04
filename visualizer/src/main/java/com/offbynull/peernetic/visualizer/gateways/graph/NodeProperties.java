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
 * Properties of a node.
 * @author Kasra Faghihi
 */
public final class NodeProperties {
    private final String text;
    private final int color;
    private final double x;
    private final double y;

    /**
     * Constructs a {@link NodeProperties} instance.
     * @param text node text
     * @param color node color (24-bit RGB)
     * @param x node x coordinate
     * @param y node y coordinate
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if top 8-bits of {@code color} are not {@code 0}
     */
    public NodeProperties(String text, int color, double x, double y) {
        Validate.notNull(text);
        Validate.isTrue((color & 0xFF000000) == 0);
        this.text = text;
        this.color = color;
        this.x = x;
        this.y = y;
    }

    /**
     * Get the node's text.
     * @return node text
     */
    public String getText() {
        return text;
    }

    /**
     * Get the node's color.
     * @return node color
     */
    public int getColor() {
        return color;
    }

    /**
     * Get the node's X coordinate.
     * @return node X coordinate
     */
    public double getX() {
        return x;
    }

    /**
     * Get the node's Y coordinate.
     * @return node Y coordinate
     */
    public double getY() {
        return y;
    }
    
}
