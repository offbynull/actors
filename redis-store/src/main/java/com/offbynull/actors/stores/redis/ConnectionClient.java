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
package com.offbynull.actors.stores.redis;

import com.offbynull.actors.shuttle.Address;
import com.offbynull.actors.stores.redis.client.ActorAccessor;
import com.offbynull.actors.stores.redis.client.TimestampQueue;
import java.io.IOException;
import org.apache.commons.lang3.Validate;
import com.offbynull.actors.stores.redis.client.Client;
import com.offbynull.actors.stores.redis.connector.Connection;

/**
 * A {@link Client} that delegates to a low-level Redis {@link Connection}.
 * @author Kasra Faghihi
 */
public final class ConnectionClient implements Client {

    private final Connection client;

    /**
     * Constructs a {@link ConnectionClient} object.
     * @param connection underlying connection
     * @throws NullPointerException if any argument is {@code null}
     */
    public ConnectionClient(Connection connection) {
        Validate.notNull(connection);
        this.client = connection;
    }
    
    @Override
    public ActorAccessor getActorAccessor(Address address) {
        Validate.notNull(address);
        return new ConnectionActorAccessor(client, address);
    }

    @Override
    public TimestampQueue getMessageCheckQueue(int num) {
        return new ConnectionTimestampQueue(client, "message", num);
    }

    @Override
    public TimestampQueue getCheckpointCheckQueue(int num) {
        return new ConnectionTimestampQueue(client, "checkpoint", num);
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
    
}
