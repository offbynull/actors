/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.redisclient;

/**
 * A low-level Redis connection exception.
 * @author Kasra Faghihi
 */
public final class ConnectionException extends Exception {
    private static final long serialVersionUID = 1L;
    
    private final boolean connectionProblem;
    
    /**
     * Constructs a {@link ConnectionException} object.
     * @param connectionProblem {@code true} if this exception was caused by connection issues, {@code false} otherwise
     * @param cause cause of this exception
     */
    public ConnectionException(boolean connectionProblem, Throwable cause) {
        super(cause);
        this.connectionProblem = connectionProblem;
    }

    /**
     * Gets the connection problem flag.
     * @return {@code true} if this exception was caused by connection issues
     */
    public boolean isConnectionProblem() {
        return connectionProblem;
    }
}
