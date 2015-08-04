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
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link GraphNodeAddHandler} that sets any newly added on to the graph to have the color white and be positioned at
 * coordinate {@code (0, 0)}. Nodes that are changes from temporary to permanent retain existing properties.
 * @author Kasra Faghihi
 */
public final class DefaultGraphNodeAddHandler implements GraphNodeAddHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultGraphNodeAddHandler.class);
    
    @Override
    public NodeProperties nodeAdded(Address graphAddress, String id, AddMode addMode, NodeProperties nodeProperties) {
        Validate.notNull(graphAddress);
        Validate.notNull(id);
        Validate.notNull(addMode);
        // nodeProperties can be null
        
        LOG.debug("Adding {} with mode {}", id, addMode);
        
        switch (addMode) {
            case PERMENANT_ADDED:
            case TEMPORARY_ADDED:
                return new NodeProperties(id, 0xFFFFFF, 0.0, 0.0);
            case TEMPORARY_TO_PERMANENT:
                return nodeProperties;
            default:
                throw new IllegalStateException(); // should never happen
        }
    }
    
}
