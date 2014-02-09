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
package com.offbynull.peernetic.router;

import java.io.Closeable;
import org.apache.commons.lang3.Validate;

/**
 * Base class for port mappers.
 * @author Kasra Faghihi
 */
public abstract class PortMapper implements Closeable {
    private PortMapperEventListener portMapperListener;

    /**
     * Construct a {@link PortMapper} object.
     * @param portMapperListener listener
     * @throws NullPointerException if any argument is {@code null}
     */
    public PortMapper(PortMapperEventListener portMapperListener) {
        Validate.notNull(portMapperListener);
        this.portMapperListener = portMapperListener;
    }

    /**
     * Map a port asynchronously.
     * @param portType port type
     * @param internalPort internal port
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any numeric argument is non-positive, or if {@code internalPort > 65535}
     */
    protected abstract void mapPort(PortType portType, int internalPort);

    /**
     * Unmap a port asynchronously.
     * @param portType port type
     * @param internalPort internal port
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any numeric argument is non-positive, or if {@code internalPort > 65535}
     */
    protected abstract void unmapPort(PortType portType, int internalPort);
    
    /**
     * Get the event listener.
     * @return event listener
     */
    protected final PortMapperEventListener getListener() {
        return portMapperListener;
    }
}
