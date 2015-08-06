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
 * Sets the properties of a node added on to a graph.
 * <p>
 * Implementations must be thread-safe.
 * @author Kasra Faghihi
 */
public interface GraphNodeAddHandler {

    /**
     * Returns properties to assign to the node once an {@link AddNode} message is processed.
     * @param graphAddress graph address that the {@link AddNode} message was sent to
     * @param id id of the node
     * @param addMode node add mode
     * @param nodeProperties existing properties on the node (only available if {@code AddNode} is set to
     * {@link AddMode#TEMPORARY_TO_PERMANENT}, otherwise {@code null})
     * @return properties to set on node (must not be {@code null})
     */
    NodeProperties nodeAdded(Address graphAddress, String id, AddMode addMode, NodeProperties nodeProperties);

    /**
     * Mode in which node was added on to graph.
     */
    public enum AddMode {
        /**
         * Temporary node has been changed to a permanent node because it was explicitly added on to the graph via an {@link AddNode}
         * message.
         */
        TEMPORARY_TO_PERMANENT,
        /**
         * Permanent node was added on to the graph because {@link AddNode} was encountered.
         */
        PERMENANT_ADDED,
        /**
         * Temporary node was added on to the graph because an {@link AddEdge} was encountered that either has it's source node id
         * or destination node id set to a node that isn't on the graph.
         */
        TEMPORARY_ADDED,
        
    }
}
