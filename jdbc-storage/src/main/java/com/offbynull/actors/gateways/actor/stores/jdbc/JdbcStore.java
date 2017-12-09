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
package com.offbynull.actors.gateways.actor.stores.jdbc;

import com.offbynull.actors.gateways.actor.SerializableActor;
import com.offbynull.actors.address.Address;
import com.offbynull.actors.shuttle.Message;
import com.offbynull.actors.gateways.actor.Store;
import com.offbynull.actors.common.BestEffortSerializer;
import static com.offbynull.actors.jdbcclient.JdbcUtils.commitFinally;
import static com.offbynull.actors.jdbcclient.JdbcUtils.retry;
import java.io.IOException;
import java.sql.Connection;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import static java.sql.ResultSet.CONCUR_UPDATABLE;
import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import javax.sql.DataSource;
import org.apache.commons.lang3.Validate;

/**
 * A storage engine that keeps all actors and messages serialized in a RDBMS.
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
 * CREATE TABLE ACTOR (
 *   ADDRESS VARCHAR(1024) NOT NULL,
 *   CHECKPOINT_DATA BLOB NOT NULL,
 *   CHECKPOINT_MESSAGE_DATA BLOB NOT NULL,
 *   CHECKPOINT_TIME BIGINT NOT NULL,        -- integer-type that can support Java's long type
 *   CHECKPOINT_INSTANCE INTEGER NOT NULL,   -- integer-type that can support Java's int type
 *   IDLE INTEGER NOT NULL,                  -- will be either 0 or 1
 *   DATA BLOB NOT NULL,
 *   PRIMARY KEY (ADDRESS)
 * );
 * 
 * CREATE TABLE MESSAGE_QUEUE (
 *   MSG_NUMBER INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), -- must be auto-incrementing, not just unique
 *   ADDRESS VARCHAR(1024) NOT NULL,
 *   DATA BLOB NOT NULL,
 *   PRIMARY KEY (MSG_NUMBER),
 *   FOREIGN KEY (ADDRESS) REFERENCES ACTOR(ADDRESS) ON DELETE CASCADE
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

    
    private static final String INSERT_ACTOR
            = "INSERT INTO ACTOR\n"
            + "    (ADDRESS, CHECKPOINT_DATA, CHECKPOINT_MESSAGE_DATA, CHECKPOINT_TIME, CHECKPOINT_INSTANCE, IDLE, DATA)\n"
            + "VALUES\n"
            + "    (?, ?, ?, ?, ?, ?, ?)\n";
    private static final String UPDATE_ACTOR_WITHOUT_CHECKPOINT
            = "UPDATE ACTOR\n"
            + "SET\n"
            + "    DATA = ?, IDLE = ?\n"
            + "WHERE\n"
            + "    ADDRESS = ? AND CHECKPOINT_INSTANCE <= ?\n";
    private static final String UPDATE_ACTOR_WITH_CHECKPOINT
            = "UPDATE ACTOR\n"
            + "SET\n"
            + "    CHECKPOINT_DATA = ?, CHECKPOINT_MESSAGE_DATA = ?, CHECKPOINT_TIME = ?, CHECKPOINT_INSTANCE = ?, DATA = ?, IDLE = ?\n"
            + "WHERE\n"
            + "    ADDRESS = ? AND CHECKPOINT_INSTANCE <= ?\n";

    @Override
    public void store(SerializableActor actor) {
        Validate.notNull(actor);
        
        Address actorAddr = actor.getSelf();
        Validate.isTrue(actorAddr.size() == 2, "Actor address has unexpected number of elements: %s", actorAddr);
        Validate.isTrue(actorAddr.getElement(0).equals(prefix), "Actor address must start with %s: %s", prefix, actorAddr);

        Validate.validState(!closed, "Store closed");
        
        byte[] actorData = serializer.serialize(actor);
        
        int checkpointInstance = actor.getCheckpointInstance();
        boolean checkpointUpdated = actor.getCheckpointUpdated();
        byte[] checkpointPayloadData;
        if (checkpointUpdated) {
            Object checkpointPayload = actor.getCheckpointPayload();
            checkpointPayloadData = serializer.serialize(new Message(actorAddr, actorAddr, checkpointPayload));
        } else {
            checkpointPayloadData = null;
        }
        
        retry(() -> {
            Validate.isTrue(!closed, "Store closed");
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);

                Instant currentInstant = Instant.now();
                long checkpointTimeout = actor.getCheckpointTimeout();
                long checkpointTime = calculateCheckpointTime(currentInstant, checkpointTimeout);
                
                String actorAddrStr = actorAddr.toString();

                
                
                // Try inserting the actor into the actors table -- initially, an actor must have a checkpoint
                if (checkpointPayloadData != null) {
                    try (PreparedStatement ps = conn.prepareStatement(INSERT_ACTOR)) {
                        ps.setString(1, actorAddrStr);
                        ps.setBytes(2, actorData);
                        ps.setBytes(3, checkpointPayloadData);
                        ps.setLong(4, checkpointTime);
                        ps.setInt(5, checkpointInstance);
                        ps.setInt(6, 1); // idle = true
                        ps.setBytes(7, actorData);
                        ps.executeUpdate();
                        return;
                    } catch (SQLException sqle) {
                        if (!sqle.getSQLState().startsWith("23505")) { // 23505 is used when already exists (unique constraint violation)
                            throw sqle;
                        }
                    } finally {
                        commitFinally(conn);
                    }
                }
                
                
                
                // If we reached this point, it means the insert failed because it already existed -- try updating existing instead
                if (checkpointPayloadData == null) {
                    try (PreparedStatement ps = conn.prepareStatement(UPDATE_ACTOR_WITHOUT_CHECKPOINT)) {
                        ps.setBytes(1, actorData);
                        ps.setInt(2, 1); // idle = true
                        ps.setString(3, actorAddrStr);
                        ps.setInt(4, checkpointInstance);
                        ps.executeUpdate();
                    } finally {
                        commitFinally(conn);
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(UPDATE_ACTOR_WITH_CHECKPOINT)) {
                        ps.setBytes(1, actorData);
                        ps.setBytes(2, checkpointPayloadData);
                        ps.setLong(3, checkpointTime);
                        ps.setInt(4, checkpointInstance);
                        ps.setBytes(5, actorData);
                        ps.setInt(6, 1); // idle = true
                        ps.setString(7, actorAddrStr);
                        ps.setInt(8, checkpointInstance);
                        ps.executeUpdate();
                    } finally {
                        commitFinally(conn);
                    }
                }
            }
        });
    }
    
    
    
    private static final String INSERT_MESSAGE = "INSERT INTO MESSAGE_QUEUE (ADDRESS, DATA) VALUES (?, ?)";
    
    @Override
    public void store(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        Validate.validState(!closed, "Store closed");
        messages.forEach(m -> {
            Address dstAddr = m.getDestinationAddress();
            Validate.isTrue(dstAddr.size() >= 2, "Actor address must have atleast 2 elements: %s", dstAddr);
            Validate.isTrue(dstAddr.getElement(0).equals(prefix), "Actor address must start with %s: %s", prefix, dstAddr);
        });

        for (Message message : messages) {
            String dstAddrStr = Address.of(prefix, message.getDestinationAddress().getElement(1)).toString();
            byte[] messageData = serializer.serialize(message);

            retry(() -> {
                Validate.isTrue(!closed, "Store closed");
                try (Connection conn = dataSource.getConnection();
                        PreparedStatement ps = conn.prepareStatement(INSERT_MESSAGE)) {
                    conn.setAutoCommit(false);
                    conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);

                    ps.setString(1, dstAddrStr);
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

    
    
    
    private static final String DELETE_ACTOR = "DELETE FROM ACTOR WHERE ADDRESS = ?";
    
    @Override
    public void discard(Address address) {
        Validate.isTrue(address.size() == 2);
        Validate.isTrue(address.getElement(0).equals(prefix));
        Validate.validState(!closed, "Store closed");
        
        String addrStr = address.toString();

        retry(() -> {
            Validate.isTrue(!closed, "Store closed");
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);

                try (PreparedStatement ps = conn.prepareStatement(DELETE_ACTOR)) {
                    ps.setString(1, addrStr);
                    ps.executeUpdate();
                } finally {
                    commitFinally(conn);
                }
            }
        });
    }

    
    
    
    @Override
    public StoredWork take() {
        Validate.validState(!closed, "Store closed");
        
        Work work = retry(() -> {
            Validate.isTrue(!closed, "Store closed");
            while (true) {
                Work checkpointWork = takeCheckpoint();
                if (checkpointWork != null) {
                    return checkpointWork;
                }

                Work msgWork = takeMessage();
                if (msgWork != null) {
                    return msgWork;
                }
            }
        });

        byte[] actorData = work.getActorData();
        byte[] messageData = work.getMessageData();

        SerializableActor actor = serializer.deserialize(actorData);
        Message msg = serializer.deserialize(messageData);
        
        actor.setCheckpointInstance(work.getCheckpointInstance());
        actor.setCheckpointUpdated(true);

        return new StoredWork(msg, actor);
    }


    // use ps.setMaxRows to make it only compute 1 row
    private static final String GET_NEXT_MESSAGE
            = "SELECT min(m.MSG_NUMBER), m.ADDRESS\n"
            + "    FROM MESSAGE_QUEUE m INNER JOIN ACTOR a ON m.ADDRESS = a.ADDRESS\n"
            + "    WHERE a.IDLE <> 0\n"
            + "    GROUP BY m.ADDRESS";
    private static final String SELECT_ACTOR_FOR_IDLE_UPDATE
            = "SELECT ADDRESS, IDLE, DATA, CHECKPOINT_INSTANCE FROM ACTOR WHERE ADDRESS = ? AND IDLE <> 0 FOR UPDATE";
    private static final String SELECT_MESSAGE_FOR_DELETE
            = "SELECT MSG_NUMBER, DATA FROM MESSAGE_QUEUE WHERE MSG_NUMBER = ? FOR UPDATE";

    private Work takeMessage() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);

            
            int msgId;
            String actorAddr;
            try (PreparedStatement ps = conn.prepareStatement(GET_NEXT_MESSAGE)) {
                ps.setMaxRows(1);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    
                    msgId = rs.getInt(1); // will be 0 if null, but will never be null because column is non-null
                    actorAddr = rs.getString(2);
                }
            } finally {
                commitFinally(conn);
            }

            
            
            byte[] actorData;
            int checkpointInstance;
            try (PreparedStatement ps = conn.prepareStatement(SELECT_ACTOR_FOR_IDLE_UPDATE, TYPE_FORWARD_ONLY, CONCUR_UPDATABLE)) {
                ps.setMaxRows(1);
                ps.setString(1, actorAddr);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { // actor was already set to processing, so try getting another message
                        return null;
                    }

                    checkpointInstance = rs.getInt(4);                         // read checkpoint instance
                    actorData = rs.getBytes(3);                                // read serialized actor data

                    rs.updateInt(2, 1);                                        // update actor so it's set to procesing

                    rs.updateRow();                                            // apply changes to DB
                }
            } finally {
                commitFinally(conn);
            }

            
            
            byte[] messageData;
            try (PreparedStatement ps = conn.prepareStatement(SELECT_MESSAGE_FOR_DELETE, TYPE_FORWARD_ONLY, CONCUR_UPDATABLE)) {
                ps.setMaxRows(1);
                ps.setInt(1, msgId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { // message was already removed, so try getting another message
                        return null;
                    }
                    
                    messageData = rs.getBytes(2);

                    rs.deleteRow(); // delete the message because we're about to start processing it
                }
            } finally {
                commitFinally(conn);
            }

            return new Work(actorData, messageData, checkpointInstance);
        }
    }


    // use ps.setMaxRows to make it only compute 1 row
    private static final String SELECT_CHECKPOINT_FOR_INSTANCE_UPDATE
            = "SELECT ADDRESS, CHECKPOINT_INSTANCE, CHECKPOINT_DATA, CHECKPOINT_MESSAGE_DATA, IDLE FROM ACTOR WHERE CHECKPOINT_TIME <= ?";

    private Work takeCheckpoint() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            
            
            long currentTime = Instant.now().toEpochMilli();
            try (PreparedStatement ps = conn.prepareStatement(SELECT_CHECKPOINT_FOR_INSTANCE_UPDATE, TYPE_FORWARD_ONLY, CONCUR_UPDATABLE)) {
                ps.setMaxRows(1);
                ps.setLong(1, currentTime);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    
                    int checkpointInstance = rs.getInt(2); // will be 0 if null, but will never be null because column is non-null
                    byte[] checkpointData = rs.getBytes(3);
                    byte[] checkpointMessageData = rs.getBytes(4);
                    
                    checkpointInstance++;
                    
                    rs.updateInt(2, checkpointInstance); // update to incremented checkpointInstance
                    rs.updateInt(5, 1);                  // update to not idle
                    rs.updateRow(); // force row to update

                    return new Work(checkpointData, checkpointMessageData, checkpointInstance);
                }
            } finally {
                commitFinally(conn);
            }
        }
    }
    
    private static final class Work {
        private final byte[] actorData;
        private final byte[] messageData;
        private final int checkpointInstance;

        private Work(byte[] actorData, byte[] messageData, int checkpointInstance) {
            Validate.notNull(actorData);
            Validate.notNull(messageData);

            this.actorData = actorData.clone();
            this.messageData = messageData.clone();
            this.checkpointInstance = checkpointInstance;
        }

        private byte[] getActorData() {
            return actorData.clone();
        }

        private byte[] getMessageData() {
            return messageData.clone();
        }

        private int getCheckpointInstance() {
            return checkpointInstance;
        }
    }
    
    @Override
    public void close() throws IOException {
        closed = true;
    }
    
    
    
    
    
    
    
    
    private static long calculateCheckpointTime(Instant currentInstant, long timeout) {
        try {
            return currentInstant.plusMillis(timeout).toEpochMilli();
        } catch (ArithmeticException ae) {
            return Long.MAX_VALUE;
        }
    }
}
