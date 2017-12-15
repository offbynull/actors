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

import com.offbynull.actors.address.Address;
import static com.offbynull.actors.jdbcclient.JdbcUtils.commitFinally;
import static com.offbynull.actors.jdbcclient.JdbcUtils.retry;
import java.sql.Connection;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import static java.sql.ResultSet.CONCUR_UPDATABLE;
import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.apache.commons.lang3.Validate;

// Remember that access by the same ID has to happen serially. This class is not designed for concurrent access by the same ID -- if it
// happens, the state for that ID may go into an unknown state. See the servlet and store classes for more information on why this is.

final class OutQueue {
    private final String prefix;
    private final DataSource dataSource;
    private final AtomicBoolean closed;

    OutQueue(String prefix, DataSource dataSource, AtomicBoolean closed) {
        Validate.notNull(prefix);
        Validate.notNull(dataSource);
        Validate.notNull(closed);
        this.prefix = prefix;
        this.dataSource = dataSource;
        this.closed = closed;
    }
    
    
    
    
    private static final String GET_OFFSET_FOR_UPDATE = "SELECT ADDRESS, OFFSET FROM SERVLET_CLIENT_OUT_OFFSET WHERE ADDRESS = ?";
    private static final String GET_LENGTH_SQL = "SELECT COUNT(*) FROM SERVLET_CLIENT_OUT_QUEUE WHERE ADDRESS = ?";
    private static final String INSERT_MESSAGE_SQL = "INSERT INTO SERVLET_CLIENT_OUT_QUEUE (ADDRESS, OFFSET, DATA) VALUES (?, ?, ?)";
    void queueOut(String id, List<byte[]> messages) {
        Validate.notNull(id);
        Validate.notNull(messages);
        Validate.noNullElements(messages);

        String clientAddrStr = Address.of(prefix, id).toString();
        
        retry(() -> {
            Validate.isTrue(!closed.get(), "Store closed");

            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);

                int queueLen;
                int queueOffset;

                try (PreparedStatement ps = conn.prepareStatement(GET_OFFSET_FOR_UPDATE, TYPE_FORWARD_ONLY, CONCUR_UPDATABLE)) {
                    ps.setString(1, clientAddrStr);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            queueOffset = rs.getInt(2);
                        } else {
                            queueOffset = 0;
                            rs.moveToInsertRow();
                            rs.updateString(1, clientAddrStr);
                            rs.updateInt(2, queueOffset);
                            rs.insertRow();
                        }
                    }
                } finally {
                    commitFinally(conn);
                }

                try (PreparedStatement ps = conn.prepareStatement(GET_LENGTH_SQL)) {
                    ps.setString(1, clientAddrStr);
                    try (ResultSet rs = ps.executeQuery()) {
                        queueLen = rs.next() ? rs.getInt(1) : 0;    // 0 if null
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(INSERT_MESSAGE_SQL)) {
                    for (byte[] message : messages) {
                        ps.setString(1, clientAddrStr);
                        ps.setInt(2, queueOffset);
                        ps.setBytes(3, message);
                        ps.executeUpdate();

                        queueOffset++;
                    }
                } finally {
                    commitFinally(conn);
                }
            }
        });
    }

    private static final String GET_MESSAGE_FOR_DELETE_SQL
            = "SELECT MSG_NUMBER, OFFSET, DATA FROM SERVLET_CLIENT_OUT_QUEUE WHERE ADDRESS = ?";
    List<byte[]> dequeueOut(String id, int offset) {
        Validate.notNull(id);

        String clientAddrStr = Address.of(prefix, id).toString();
        
        List<byte[]> messages = retry(() -> {
            Validate.isTrue(!closed.get(), "Store closed");
            
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);

                TreeMap<Integer, byte[]> ret = new TreeMap<>(); // used for sorting, because ORDER BY prevents ResultSet.deletRow()
                try (PreparedStatement ps = conn.prepareStatement(GET_MESSAGE_FOR_DELETE_SQL, TYPE_FORWARD_ONLY, CONCUR_UPDATABLE)) {
                    ps.setString(1, clientAddrStr);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int msgOffset = rs.getInt(2);
                            byte[] msgData = rs.getBytes(3);

                            if (msgOffset < offset) { // delete it if it's < offset
                                rs.deleteRow();
                            } else { // return it if it's >= to offset
                                ret.put(msgOffset, msgData);
                            }
                        }
                    }
                } finally {
                    commitFinally(conn);
                }
                
                try (PreparedStatement ps = conn.prepareStatement(GET_OFFSET_FOR_UPDATE, TYPE_FORWARD_ONLY, CONCUR_UPDATABLE)) {
                    ps.setString(1, clientAddrStr);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && !ret.isEmpty()) {
                            int newOffset = ret.lastKey();
                            rs.updateInt(2, newOffset + 1);
                            rs.updateRow();
                        }
                    }
                } finally {
                    commitFinally(conn);
                }

                // Validate data being returned -- offsets being returned must start at offset and be contiguous
                if (!ret.isEmpty()) {
                    Validate.validState(ret.firstKey() == offset, "Gap detected in data -- expected first offset of %d but got %d",
                            offset, ret.firstKey());

                    int offsetCheckVal = offset;
                    for (Integer msgOffset : ret.keySet()) {
                        Validate.validState(msgOffset == offsetCheckVal, "Gap detected in data -- expected offset of %d but got %d",
                                offsetCheckVal, msgOffset);
                        offsetCheckVal++;
                    }
                }
                
                return new ArrayList<>(ret.values());
            }
        });
        
        return messages;
    }
}
