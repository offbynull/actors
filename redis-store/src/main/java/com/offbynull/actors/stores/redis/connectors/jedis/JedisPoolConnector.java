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
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import com.offbynull.actors.stores.redis.connector.Connection;
import com.offbynull.actors.stores.redis.connector.Connector;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Jedis connector backed by a {@link JedisPool}. This class is thread-safe.
 * <p>
 * All {@link Connection} objects derived from this {@link Connector} point to the same Redis server. If this {@link Connector} is closed,
 * any generated {@link Connection}s are also closed.
 * @author Kasra Faghihi
 */
public final class JedisPoolConnector implements Connector {

    private final JedisPool pool;
    private final AtomicBoolean closed;
    
    /**
     * Constructs a {@link JedisConnector} object.
     * @param host redis host
     * @param port redis port
     * @param idleCount number of idle connections to keep pooled
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code idleCount} is negative or {@code port} is an invalid port number
     */
    public JedisPoolConnector(String host, int port, int idleCount) {
        Validate.notNull(host);
        Validate.isTrue(port > 0 && port <= 65535);
        Validate.isTrue(idleCount >= 0);
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMinIdle(idleCount);
        poolConfig.setMaxIdle(idleCount);
        poolConfig.setMaxTotal(Integer.MAX_VALUE);
        poolConfig.setMaxWaitMillis(-1L);
        poolConfig.setMinEvictableIdleTimeMillis(60 * 1000L);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setFairness(false);
        poolConfig.setNumTestsPerEvictionRun(1);

        this.pool = new JedisPool(poolConfig, host, port);
        this.closed = new AtomicBoolean();
    }

    @Override
    public Connection getConnection() {
        Validate.validState(!closed.get(), "Closed");
        
        Jedis jedis = pool.getResource(); // throws illegalstateexc if closed
        
        Connection client = new JedisConnection(jedis, closed);
        return client;
    }

    @Override
    public void close() throws IOException {
        closed.set(true);
        pool.close(); // closes all connections generated from this pool
    }
    
}
