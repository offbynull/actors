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
 * Message to change the label on a node.
 * @author Kasra Faghihi
 */
public final class LabelNode implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String id;
    private final String label;

    /**
     * Constructs a {@link LabelNode} instance.
     * @param id id of node to be styled
     * @param label label to apply to node
     * @throws NullPointerException if any argument is {@code null}
     */
    public LabelNode(String id, String label) {
        Validate.notNull(id);
        Validate.notNull(label);
        this.id = id;
        this.label = label;
    }

    /**
     * Get id of node being styled.
     * @return node id
     */
    public String getId() {
        return id;
    }

    /**
     * Get label to apply to node.
     * @return label to apply
     */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "LabelNode{" + "id=" + id + ", label=" + label + '}';
    }
    
}
