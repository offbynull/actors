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

import com.offbynull.peernetic.core.shuttle.Address;

/**
 * Sets the properties of a node removed from a graph.
 * <p>
 * Implementations must be thread-safe.
 * @author Kasra Faghihi
 */
public interface GraphNodeRemoveHandler {
    
    /**
     * Returns properties to assign to the node once an {@link RemoveNode} message is processed. Note that the return value is only used
     * when {@code mode} is set to {@link RemoveMode#PERMANENT_TO_TEMPORARY}.
     * @param graphAddress graph address that the {@link RemoveNode} message was sent to
     * @param id id of the node
     * @param removeMode node remove mode
     * @param nodeProperties existing properties on the node
     * @return properties to set on node (must not be {@code null} if {@code mode} is set to {@link RemoveMode#PERMANENT_TO_TEMPORARY})
     */
    NodeProperties nodeRemoved(Address graphAddress, String id, RemoveMode removeMode, NodeProperties nodeProperties);
    
    /**
     * Mode in which node was removed from the graph.
     */
    public enum RemoveMode {
        /**
         * Permanent node has been changed to a temporary node because {@link RemoveNode} was encountered but there are still edges anchored
         * to that node.
         */
        PERMANENT_TO_TEMPORARY,
        /**
         * Permanent node (node that was explicitly added via {@link AddNode}) has been deleted because {@link RemoveNode} was
         * encountered and no edge anchors to that node.
         */
        PERMENANT_REMOVED,
        /**
         * Temporary node (node that was implicitly on the graph because it was needed for an edge) has been deleted because
         * {@link RemoveEdge} was encountered and no more edges are anchored to that node anymore.
         */
        TEMPORARY_REMOVED
    }
}
