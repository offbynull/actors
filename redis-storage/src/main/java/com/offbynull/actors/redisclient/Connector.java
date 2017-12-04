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

import java.io.Closeable;

/**
 * A low-level Redis connector.
 * <p>
 * Implementations may or may not be bound to to this connector. That means that if this {@link Connector} is closed, the generated
 * {@link Connection}s is may also be closed.
 * <p>
 * Implementations must be thread-safe.
 * @author Kasra Faghihi
 */
public interface Connector extends Closeable {
    /**
     * Get a {@link Connection}.
     * @return connection
     * @throws IllegalStateException if closed
     */
    Connection getConnection();
}
