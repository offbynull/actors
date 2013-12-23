/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.overlay.common.visualizer;

import org.apache.commons.lang3.Validate;

/**
 * Removes a node.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class RemoveNodeCommand<A> implements Command<A> {
    private A node;

    /**
     * Constructs a {@link RemoveNodeCommand} object.
     * @param node node
     * @throws NullPointerException if any arguments are {@code null}
     */
    public RemoveNodeCommand(A node) {
        Validate.notNull(node);
        
        this.node = node;
    }

    /**
     * Get node.
     * @return node.
     */
    public A getNode() {
        return node;
    }
}
