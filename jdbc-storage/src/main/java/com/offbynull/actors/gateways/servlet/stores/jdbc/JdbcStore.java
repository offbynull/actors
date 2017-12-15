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
package com.offbynull.actors.gateways.servlet.stores.jdbc;

import com.offbynull.actors.common.BestEffortSerializer;
import com.offbynull.actors.gateways.servlet.Store;
import com.offbynull.actors.shuttle.Message;
import java.io.IOException;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import static java.util.stream.Collectors.toList;
import javax.sql.DataSource;
import org.apache.commons.lang3.Validate;

/**
 * A storage engine that keeps messages going to / coming from HTTP clients in a RDBMS. This implementation does not have an eviction
 * policy.
 * <p>
 * This storage engine will work with any JDBC driver that supports...
 * <ul>
 * <li>TRANSACTION_READ_COMMITTED transaction level</li>
 * <li>SELECT FOR UPDATE statements and CONCUR_UPDATABLE ResultSet concurrency</li>
 * <li>SQLSTATE codes</li>
 * </ul>
 * The schema your JDBC {@link Connection}s point to must have the tables below. These DDL statements are for Apache Derby -- you'll need
 * to tweak them to match for your RDBMS vendor. Performance is your responsibility -- it's up to you to tune table, schema, and database
 * options to make access performant.
 * <pre>
 * CREATE TABLE SERVLET_CLIENT_IN_QUEUE (
 *   MSG_NUMBER INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
 *   ADDRESS VARCHAR(1024) NOT NULL,
 *   OFFSET INTEGER NOT NULL,
 *   DATA BLOB NOT NULL,
 *   PRIMARY KEY (MSG_NUMBER),
 *   UNIQUE (ADDRESS, OFFSET)
 * );
 * CREATE TABLE SERVLET_CLIENT_IN_OFFSET (
 *   ADDRESS VARCHAR(1024) NOT NULL,
 *   OFFSET INTEGER NOT NULL,
 *   PRIMARY KEY (ADDRESS)
 * );
 * CREATE TABLE SERVLET_CLIENT_OUT_QUEUE (
 *   MSG_NUMBER INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
 *   ADDRESS VARCHAR(1024) NOT NULL,
 *   OFFSET INTEGER NOT NULL,
 *   DATA BLOB NOT NULL,
 *   PRIMARY KEY (MSG_NUMBER),
 *   UNIQUE (ADDRESS, OFFSET)
 * );
 * CREATE TABLE SERVLET_CLIENT_OUT_OFFSET (
 *   ADDRESS VARCHAR(1024) NOT NULL,
 *   OFFSET INTEGER NOT NULL,
 *   PRIMARY KEY (ADDRESS)
 * );
 * </pre>
 * @author Kasra Faghihi
 */
public final class JdbcStore implements Store {

    //
    // THIS HAS BEEN TESTED WITH POSTGRES 9.6 AND DERBY 10.14.1.0
    //
    // To support Postgres, I had to workaround the following issues...
    //    cannot capture subtypes of SQLException -- have to capture SQLException directly
    //    cannot call setBlob/getBlob on blobs, must call setBytes/getBytes (blob type in postgres is bytea)
    //    SELECT FOR UPDATES need to include the pk column even though they aren't needed, otherwise you can't update/delete the row
    //
    
    private final BestEffortSerializer serializer;
    
    private final InQueue inQueue;
    private final OutQueue outQueue;
    
    private final AtomicBoolean closed;
    
    /**
     * Creates a {@link JdbcStore} object.
     * @param prefix prefix for the actor gateway that this storage engine belongs to
     * @param dataSource JDBC data source to generate connections
     * @return new JDBC store
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code port} is invalid
     */
    public static JdbcStore create(String prefix, DataSource dataSource) {
        Validate.notNull(prefix);
        Validate.notNull(dataSource);
        return new JdbcStore(prefix, dataSource);
    }

    private JdbcStore(String prefix, DataSource dataSource) {
        Validate.notNull(prefix);
        Validate.notNull(dataSource);
        this.serializer = new BestEffortSerializer();
        this.closed = new AtomicBoolean();
        this.inQueue = new InQueue(prefix, dataSource, closed);
        this.outQueue = new OutQueue(prefix, dataSource, closed);
    }

    @Override
    public void queueOut(String id, List<Message> messages) {
        Validate.notNull(id);
        Validate.notNull(messages);
        Validate.noNullElements(messages);

        List<byte[]> serializedMessages = messages.stream()
                .map(m -> serializer.serialize(m))
                .collect(toList());
        outQueue.queueOut(id, serializedMessages);
    }

    @Override
    public List<Message> dequeueOut(String id, int offset) {
        Validate.notNull(id);
        Validate.isTrue(offset >= 0);

        List<byte[]> serializedMessages = outQueue.dequeueOut(id, offset);
        return serializedMessages.stream()
                .map(d -> (Message) serializer.deserialize(d))
                .collect(toList());
    }

    @Override
    public void queueIn(String id, int offset, List<Message> messages) {
        Validate.notNull(id);
        Validate.isTrue(offset >= 0);
        Validate.noNullElements(messages);

        List<byte[]> serializedMessages = messages.stream()
                .map(m -> serializer.serialize(m))
                .collect(toList());
        inQueue.queueIn(id, offset, serializedMessages);
    }

    @Override
    public List<Message> dequeueIn(String id) {
        Validate.notNull(id);

        List<byte[]> serializedMessages = inQueue.dequeueIn(id);
        return serializedMessages.stream()
                .map(d -> (Message) serializer.deserialize(d))
                .collect(toList());
    }

    @Override
    public void close() throws IOException {
        closed.set(true);
    }
}
