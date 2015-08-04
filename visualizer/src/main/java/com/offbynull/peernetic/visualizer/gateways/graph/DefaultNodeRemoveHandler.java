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
 * An implementation of {@link GraphNodeRemoveHandler} that sets always returns the existing properties of a node.
 * @author Kasra Faghihi
 */
public final class DefaultNodeRemoveHandler implements GraphNodeRemoveHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(DefaultNodeRemoveHandler.class);

    @Override
    public NodeProperties nodeRemoved(Address graphAddress, String id, RemoveMode removeMode, NodeProperties nodeProperties) {
        Validate.notNull(graphAddress);
        Validate.notNull(id);
        Validate.notNull(removeMode);
        // nodeProperties can be null
        
        LOG.debug("Removing {} with mode {}", id, removeMode);
        
        return nodeProperties;
    }
    
}
