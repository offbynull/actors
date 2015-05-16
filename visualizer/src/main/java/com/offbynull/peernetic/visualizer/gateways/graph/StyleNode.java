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

import javafx.scene.control.Label;
import org.apache.commons.lang3.Validate;

/**
 * Message to apply a JavaFX CSS effect to a node.
 * @author Kasra Faghihi
 */
public final class StyleNode {
    private final String id;
    private final String style;

    /**
     * Constructs a {@link StyleNode} instance.
     * @param id id of node to be styled
     * @param style JavaFX CSS style to apply to node (node is a JavaFX {@link Label})
     * @throws NullPointerException if any argument is {@code null}
     */
    public StyleNode(String id, String style) {
        Validate.notNull(id);
        Validate.notNull(style);
        this.id = id;
        this.style = style;
    }

    /**
     * Get id of node being styled.
     * @return node id
     */
    public String getId() {
        return id;
    }

    /**
     * Get JavaFX CSS style to apply to node (node is a JavaFX {@link Label}).
     * @return style to apply
     */
    public String getStyle() {
        return style;
    }
    
}
