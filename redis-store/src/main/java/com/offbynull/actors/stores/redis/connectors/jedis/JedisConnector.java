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
package com.offbynull.actors.stores.redis.connectors.jedis;

import java.io.IOException;
import org.apache.commons.lang3.Validate;
import redis.clients.jedis.Jedis;
import com.offbynull.actors.stores.redis.connector.Connection;
import com.offbynull.actors.stores.redis.connector.Connector;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.IOUtils;

/**
 * Jedis connector. This class is thread-safe.
 * <p>
 * All {@link Connection} objects derived from this {@link Connector} point to the same Redis server. If this {@link Connector} is closed,
 * the generated {@link Connection}s are also closed.
 * @author Kasra Faghihi
 */
public final class JedisConnector implements Connector {
    
    private final String host;
    private final int port;
    
    private final WeakHashMap<JedisConnection, Object> conns;
    private final AtomicBoolean closed;
    
    /**
     * Constructs a {@link JedisConnector} object.
     * @param host redis host
     * @param port redis port
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code port} is an invalid port number
     */
    public JedisConnector(String host, int port) {
        Validate.notNull(host);
        Validate.isTrue(port > 0 && port <= 65535);
        this.host = host;
        this.port = port;
        this.conns = new WeakHashMap<>();
        this.closed = new AtomicBoolean();
    }

    @Override
    public Connection getConnection() {
        Validate.validState(!closed.get(), "Closed");
        
        Jedis jedis = new Jedis(host, port);
        JedisConnection conn = new JedisConnection(jedis, closed);
        
        conns.put(conn, null);
        
        return conn;
    }

    @Override
    public void close() throws IOException {
        closed.set(true);
        conns.keySet().forEach(jc -> IOUtils.closeQuietly(jc));
    }
    
}
