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
import java.util.concurrent.Future;
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
     * Map a port. If the port has already been mapped, is in the process of being mapped, or is in the process of being unmapped, then
     * the future will return an exception.
     * @param port internal port
     * @return future that can be used to check if the mapping has been acquired (not cancelable)
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any numeric argument is non-positive, or if {@code internalPort > 65535}
     */
    public abstract Future<MappedPort> mapPort(Port port);

    /**
     * Unmap a port. If the port hasn't been mapped, is in the process of being mapped, or is in the process of already being unmapped, then
     * the future will return an exception.
     * @param port internal port
     * @return future that can be used to check if the mapping has been released (not cancelable)
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any numeric argument is non-positive, or if {@code internalPort > 65535}
     */
    public abstract Future<Void> unmapPort(Port port);
    
    /**
     * Get the event listener.
     * @return event listener
     */
    protected final PortMapperEventListener getListener() {
        return portMapperListener;
    }
}
