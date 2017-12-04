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
import static com.offbynull.actors.jdbcclient.JdbcUtils.commitFinally;
import static com.offbynull.actors.jdbcclient.JdbcUtils.retry;
import com.offbynull.actors.shuttle.Address;
import com.offbynull.actors.shuttle.Message;
import java.io.IOException;
import java.sql.Connection;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import static java.sql.ResultSet.CONCUR_UPDATABLE;
import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
import java.sql.SQLException;
import java.util.List;
import java.util.TreeMap;
import static java.util.stream.Collectors.toList;
import javax.sql.DataSource;
import org.apache.commons.lang3.Validate;

/**
 * A storage engine that keeps messages for HTTP clients in a RDBMS. This implementation does not evict messages -- messages are only
 * removed after they're read.
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
 * CREATE TABLE HTTP_CLIENT_QUEUE (
 *   MSG_NUMBER INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), -- must be auto-incrementing, not just unique
 *   ADDRESS VARCHAR(1024) NOT NULL,
 *   DATA BLOB NOT NULL,
 *   PRIMARY KEY (MSG_NUMBER)
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
    
    private final String prefix;
    private final DataSource dataSource;
    private final BestEffortSerializer serializer;
    
    private volatile boolean closed;
    
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
        this.prefix = prefix;
        this.dataSource = dataSource;
        this.serializer = new BestEffortSerializer();
    }
    
    private static final String INSERT_MESSAGE = "INSERT INTO HTTP_CLIENT_QUEUE (ADDRESS, DATA) VALUES (?, ?)";
    
    @Override
    public void write(String id, List<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        Validate.validState(!closed, "Store closed");
        
        Address clientAddr = Address.of(prefix, id);
                
        messages.stream().forEach(m -> {
            Address dstAddr = m.getDestinationAddress();
            Validate.isTrue(dstAddr.size() >= 2, "Actor address must have atleast 2 elements: %s", dstAddr);
            Validate.isTrue(clientAddr.isPrefixOf(dstAddr), "Actor address must start with %s: %s", clientAddr, dstAddr);
        });

        String clientAddrStr = clientAddr.toString();
        for (Message message : messages) {
            byte[] messageData = serializer.serialize(message);

            retry(() -> {
                Validate.isTrue(!closed, "Store closed");
                try (Connection conn = dataSource.getConnection();
                        PreparedStatement ps = conn.prepareStatement(INSERT_MESSAGE)) {
                    conn.setAutoCommit(false);
                    conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);

                    ps.setString(1, clientAddrStr);
                    ps.setBytes(2, messageData);
                    ps.executeUpdate();
                    commitFinally(conn);
                } catch (SQLException sqle) {
                    if (!sqle.getSQLState().startsWith("23503")) { // 23505 is used when no actor with this address exists (foreign key)
                        throw sqle;
                    }
                }
            });
        }
    }

    // NOTE: We can't have ORDER BY MSG_NUMBER in this statement if we're going to do FOR UPDATE. As such, we return the MSG_NUMBER and we
    // add it as the key of a TreeMap to manually sort the messages as we pull them in. Remember that MSG_NUMBER is a auto-incrementing
    // integer used for the primary key.
    private static final String SELECT_MESSAGE_FOR_DELETE
            = "SELECT MSG_NUMBER, ADDRESS, DATA FROM HTTP_CLIENT_QUEUE WHERE ADDRESS = ? FOR UPDATE";
    
    @Override
    public List<Message> read(String id) {
        String clientAddrStr = Address.of(prefix, id).toString();
        
        List<Message> messages = retry(() -> {
            Validate.isTrue(!closed, "Store closed");
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(SELECT_MESSAGE_FOR_DELETE, TYPE_FORWARD_ONLY, CONCUR_UPDATABLE)) {
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);

                ps.setString(1, clientAddrStr);
                
                TreeMap<Integer, byte[]> messageDatas = new TreeMap<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int messageNum = rs.getInt(1);
                        byte[] messageData = rs.getBytes(3);
                        messageDatas.put(messageNum, messageData);
                        
                        rs.deleteRow();
                    }
                } finally {
                    commitFinally(conn);
                }
                
                return messageDatas.values().stream()
                        .map(d -> (Message) serializer.deserialize(d))
                        .collect(toList());
            }
        });
        
        return messages;
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }
    
}
