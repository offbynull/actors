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

import com.offbynull.actors.stores.redis.client.Client;
import com.offbynull.actors.stores.redis.client.ClientFactory;
import com.offbynull.actors.stores.redis.connector.Connection;
import com.offbynull.actors.stores.redis.connector.Connector;
import java.io.IOException;
import org.apache.commons.lang3.Validate;

/**
 * A {@link ClientFactory} that generates clients that delegates to a low-level Redis {@link Connector}.
 * @author Kasra Faghihi
 */
public final class ConnectorClientFactory implements ClientFactory {

    private final Connector connector;

    /**
     * Constructs a {@link ConnectorClientFactory} object.
     * @param connector underlying connector
     * @throws NullPointerException if any argument is {@code null}
     */
    public ConnectorClientFactory(Connector connector) {
        Validate.notNull(connector);
        this.connector = connector;
    }
    
    @Override
    public Client getClient() {
        Connection conn = connector.getConnection();
        return new ConnectionClient(conn);
    }

    @Override
    public void close() throws IOException {
        connector.close();
    }
    
}
