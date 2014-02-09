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
 * Exception that signals that the port could not be opened by {@link PortMapper#mapPort(com.offbynull.peernetic.router.PortType) }.
 * @author Kasra Faghihi
 */
public final class PortMapException extends RuntimeException {

    /**
     * Construct a {@link UnableToOpenPortException}.
     */
    public PortMapException() {
    }

    /**
     * Construct a {@link UnableToOpenPortException} with a message.
     * @param message message
     */
    public PortMapException(String message) {
        super(message);
    }

    /**
     * Construct a {@link UnableToOpenPortException} with a message and a cause.
     * @param message message
     * @param cause cause
     */
    public PortMapException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct a {@link UnableToOpenPortException} with a cause.
     * @param cause cause
     */
    public PortMapException(Throwable cause) {
        super(cause);
    }
    
}
