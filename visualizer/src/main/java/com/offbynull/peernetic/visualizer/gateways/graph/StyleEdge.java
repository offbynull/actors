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
import javafx.scene.shape.Line;
import org.apache.commons.lang3.Validate;

/**
 * Message to apply a JavaFX CSS effect to an edge.
 * @author Kasra Faghihi
 */
public final class StyleEdge implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String fromId;
    private final String toId;
    private final String style;

    /**
     * Constructs a {link StyleEdge} instance.
     * @param fromId id of node that edge starts from
     * @param toId to id of node that edge ends at
     * @param style JavaFX CSS style to apply to node (node is a JavaFX {@link Line})
     * @throws NullPointerException if any argument is {@code null}
     */
    public StyleEdge(String fromId, String toId, String style) {
        Validate.notNull(fromId);
        Validate.notNull(toId);
        Validate.notNull(style);
        
        this.fromId = fromId;
        this.toId = toId;
        this.style = style;
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
     * Get JavaFX CSS style to apply to node (node is a JavaFX {@link Line}).
     * @return style to apply
     */
    public String getStyle() {
        return style;
    }
    
}
