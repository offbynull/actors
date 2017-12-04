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
package com.offbynull.actors.redisclients.test;

import com.offbynull.actors.redisclient.Connection;
import com.offbynull.actors.redisclient.Connector;
import com.offbynull.actors.redisclients.test.TestConnection.Item;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.Validate;

/**
 * Test connector. This class is thread-safe.
 * <p>
 * All {@link Connection} objects derived from this {@link Connector} point to the same database. If this {@link Connector} is closed, any
 * generated {@link Connection}s are also closed.
 * @author Kasra Faghihi
 */
public final class TestConnector implements Connector {
    
    private final Map<String, Item> keyspace = new HashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    
    @Override
    public Connection getConnection() {
        Validate.validState(!closed.get(), "Closed");
        return new TestConnection(keyspace, closed);
    }

    @Override
    public void close() throws IOException {
        closed.set(true);
    }
    
}
