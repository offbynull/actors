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

/**
 * An interface used for listening to events from {@link PortMapper}.
 * @author Kasra Faghihi
 */
public interface PortMapperEventListener {
    /**
     * Indicates that a port mapping request completed successfully.
     * @param mappedPort mapped port
     * @throws NullPointerException if any argument is {@code null}
     */
    void mappingCreationSuccessful(MappedPort mappedPort);
    /**
     * Indicates that a port mapping request wasn't successful.
     * @param portType port type
     * @param internalPort internal port
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code internalPort < 1 || > 65535}
     */
    void mappingCreationFailed(PortType portType, int internalPort);

    /**
     * Indicates that a port mapping has changed.
     * @param oldMappedPort old mapped port
     * @param newMappedPort new mapped port
     * @throws NullPointerException if any argument is {@code null}
     */
    void mappingChanged(MappedPort oldMappedPort, MappedPort newMappedPort);

    /**
     * Indicates that a port mapping has been lost.
     * @param oldMappedPort old mapped port
     * @throws NullPointerException if any argument is {@code null}
     */
    void mappingLost(MappedPort oldMappedPort);
}
