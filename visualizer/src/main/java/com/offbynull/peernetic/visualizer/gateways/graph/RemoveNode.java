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
 * Remove a node from the graph. Note that if the node being removed still has an edge connecting to it, it'll remain on the graph as a
 * temporary node. Temporary nodes are on the graph for as long as there's an edge connected. Temporary nodes cannot be manipulated unless
 * an {@link AddNode} command explicitly adds them back in.
 * @author Kasra Faghihi
 */
public final class RemoveNode implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String id;
    private final boolean removeAsFrom;
    private final boolean removeAsTo;

    /**
     * Constructs a {@link RemoveNode} instance.
     * @param id id of node to remove
     * @param removeAsFrom remove edges where this node is the from/source
     * @param removeAsTo remove edges where this node is the to/destination
     * @throws NullPointerException if any argument is {@code null}
     */
    public RemoveNode(String id, boolean removeAsFrom, boolean removeAsTo) {
        Validate.notNull(id);
        this.id = id;
        this.removeAsFrom = removeAsFrom;
        this.removeAsTo = removeAsTo;
    }

    /**
     * Constructs a {@link RemoveNode} instance that does not attempt to remove any edges. Equivalent to calling
     * {@code new RemoveNode(id, false, false)}.
     * @param id id of node to remove
     * @throws NullPointerException if any argument is {@code null}
     */
    public RemoveNode(String id) {
        this(id, false, false);
    }

    /**
     * Get id of node to be removed.
     * @return node id
     */
    public String getId() {
        return id;
    }

    /**
     * Flag indicating if this command should remove edges where this node is the from/source.
     * @return {@code true} if this command should remove edges where this node is the from/source, {@code false} otherwise
     */
    public boolean isRemoveAsFrom() {
        return removeAsFrom;
    }

    /**
     * Flag indicating if this command should remove edges where this node is the to/destination.
     * @return {@code true} if this command should remove edges where this node is the to/destination, {@code false} otherwise
     */
    public boolean isRemoveAsTo() {
        return removeAsTo;
    }

    @Override
    public String toString() {
        return "RemoveNode{" + "id=" + id + ", removeAsFrom=" + removeAsFrom + ", removeAsTo=" + removeAsTo + '}';
    }

}
