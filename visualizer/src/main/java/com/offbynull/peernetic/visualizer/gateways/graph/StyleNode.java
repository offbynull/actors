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
 * Message to change a node's style.
 * @author Kasra Faghihi
 */
public final class StyleNode implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String id;
    private final int color;

    /**
     * Constructs a {@link StyleNode} instance.
     * @param id id of node to be styled
     * @param color 24-bit RGB color value to apply to edge (top 8-bits, usually used as alpha, must be 0)
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if top 8-bits of {@code color} are not {@code 0}, or if {@code width <= 0}
     */
    public StyleNode(String id, int color) {
        Validate.notNull(id);
        Validate.isTrue((color & 0xFF000000) == 0);
        this.id = id;
        this.color = color;
    }

    /**
     * Get id of node being styled.
     * @return node id
     */
    public String getId() {
        return id;
    }

    /**
     * Get 24-bit color to apply to node.
     * @return color to apply
     */
    public int getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "StyleNode{" + "id=" + id + ", color=" + color + '}';
    }
    
}
