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
package com.offbynull.actors.gateways.servlet.stores.redis;

import com.offbynull.actors.common.BestEffortSerializer;
import com.offbynull.actors.gateways.servlet.Store;
import com.offbynull.actors.redisclient.Connection;
import com.offbynull.actors.redisclient.Connector;
import static com.offbynull.actors.redisclient.RedisUtils.retry;
import com.offbynull.actors.redisclients.jedis.JedisPoolConnector;
import com.offbynull.actors.shuttle.Address;
import com.offbynull.actors.shuttle.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * A storage engine that keeps messages for a HTTP client serialized in Redis.
 * <p>
 * This storage engine makes use of Redis lists as queues to keep track of which HTTP client gets which message.
 * <p>
 * This storage engine has been tested with a non-clustered Redis instance, but has been designed in such a way that it can be extended
 * to work with a clustered Redis instance: Redis hash tags are used to keep keys that are accessed together on the same Redis node, such
 * that they can all be queried/changed consistently in a WATCH/MULTI/EXEC transaction. These WATCH/MULTI/EXEC transactions also make sure
 * that a failure doesn't happen from data migrating between nodes.
 * @author Kasra Faghihi
 */
public final class RedisStore implements Store {

    // This storage engine has been designed to work with a single Redis server, but can easily be extended to work with a Redis cluster.

    private final String prefix;
    private final Connector connector;
    private final BestEffortSerializer serializer;
    private final long timeout;

    private volatile boolean closed;

    /**
     * Creates a {@link RedisStore} object. Equivalent to calling {@code create(prefix, host, port, 10, 60000L) }.
     * @param prefix prefix for the actor gateway that this storage engine belongs to
     * @param host redis host
     * @param port redis port
     * @return new redis store
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code port} is invalid
     */
    public static RedisStore create(String prefix, String host, int port) {
        return create(prefix, host, port, 10, 60000L);
    }
    
    /**
     * Creates a {@link RedisStore} object. Equivalent to calling
     * {@code create(prefix, new JedisPoolConnector(host, port, cacheCount), timeout)}.
     * @param prefix prefix for the actor gateway that this storage engine belongs to
     * @param host redis host
     * @param port redis port
     * @param cacheCount number of cached connections to the redis server
     * @param timeout amount of message queue for an id will be retained without more messages being added to it (in milliseconds)
     * @return new redis store
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code port} is invalid, or {@code cacheCount < 0}, or {@code timestampQueueCount <= 0}, or
     * {@code timeout < 0}
     */
    public static RedisStore create(String prefix, String host, int port, int cacheCount, long timeout) {
        Validate.notNull(prefix);
        Validate.notNull(host);
        Validate.isTrue(port > 0 && port <= 65535);
        Validate.isTrue(cacheCount >= 0);
        
        return create(prefix, new JedisPoolConnector(host, port, cacheCount), timeout);
    }

    /**
     * Creates a {@link RedisStore} object.
     * @param prefix prefix for the actor gateway that this storage engine belongs to
     * @param connector connector to use for creating redis connections
     * @param timeout amount of message queue for an id will be retained without more messages being added to it (in milliseconds)
     * @return new redis store
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code timeout < 0}
     */
    public static RedisStore create(String prefix, Connector connector, long timeout) {
        Validate.notNull(prefix);
        Validate.notNull(connector);
        Validate.isTrue(timeout >= 0L);
        return new RedisStore(prefix, connector, timeout);
    }
    
    private RedisStore(String prefix, Connector connector, long timeout) {
        Validate.notNull(prefix);
        Validate.notNull(connector);
        Validate.notNull(timeout);
        Validate.isTrue(timeout >= 0L);

        this.prefix = prefix;
        this.connector = connector;
        this.serializer = new BestEffortSerializer();
        this.timeout = timeout;
    }

    @Override
    public void write(String id, List<Message> messages) {
        Validate.notNull(id);
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        Validate.validState(!closed, "Store closed");
        
        Address clientAddr = Address.of(prefix, id);
                
        messages.stream().forEach(m -> {
            Address dstAddr = m.getDestinationAddress();
            Validate.isTrue(dstAddr.size() >= 2, "Actor address must have atleast 2 elements: %s", dstAddr);
            Validate.isTrue(clientAddr.isPrefixOf(dstAddr), "Actor address must start with %s: %s", clientAddr, dstAddr);
        });

        for (Message message : messages) {
            byte[] messageData = serializer.serialize(message);

            retry(() -> {
                Validate.validState(!closed, "Store closed");
                
                try (Connection connection = connector.getConnection()) {
                    MessageQueue messageQueue = new MessageQueue(connection, clientAddr, timeout);
                    messageQueue.putMessage(messageData);
                }
            });
        }
    }

    @Override
    public List<Message> read(String id) {
        Validate.notNull(id);
        Validate.validState(!closed, "Store closed");
        
        Address clientAddr = Address.of(prefix, id);
        
        List<Message> ret = new ArrayList<>();
        
        while (true) {
            byte[] messageData = retry(() -> {
                Validate.validState(!closed, "Store closed");
                try (Connection connection = connector.getConnection()) {
                    MessageQueue messageQueue = new MessageQueue(connection, clientAddr, timeout);
                    return messageQueue.take();
                }
            });
            
            if (messageData == null) {
                break;
            }
            
            Message message = serializer.deserialize(messageData);
            ret.add(message);
        }
        
        return ret;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        connector.close();
    }
}
