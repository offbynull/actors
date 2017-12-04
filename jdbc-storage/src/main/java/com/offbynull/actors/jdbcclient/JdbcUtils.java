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
package com.offbynull.actors.jdbcclient;

import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JDBC client utilities.
 * @author Kasra Faghihi
 */
public final class JdbcUtils {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcUtils.class);

    private JdbcUtils() {
        // do nothing
    }
    
    
    
    /**
     * Commit the connection, if not {@code null}.
     * @param conn connection to commit
     * @throws SQLException on SQL exception
     */
    public static void commitFinally(Connection conn) throws SQLException {
        if (conn != null) {
            conn.commit();
        }
    }
    
    
    
    
    
    /**
     * Retry a JDBC operation.
     * @param <V> return type
     * @param block retry block
     * @return return value
     */
    public static <V> V retry(RetryReturnBlock<V> block) {
        while (true) {
            try {
                return block.run();               
            } catch (SQLException sqle) {
                boolean connProblem = sqle.getSQLState().startsWith("08"); // 08xxx class of statecodes indicate connection problems
                if (connProblem) {
                    LOGGER.error("Connection problem encountered, retrying...", sqle);
                } else {
                    LOGGER.error("Non-connection problem encountered", sqle);
                    throw new IllegalStateException(sqle);
                }
            }
        }
    }
    
    /**
     * Retry block with a return value.
     * @param <V> return type
     */
    public interface RetryReturnBlock<V> {
        /**
         * Retry block.
         * @return return value
         * @throws SQLException sql error
         */
        V run() throws SQLException;
    }

    /**
     * Retry a JDBC operation.
     * @param block retry block
     */
    public static void retry(RetryBlock block) {
        while (true) {
            try {
                block.run();
                return;        
            } catch (SQLException sqle) {
                boolean connProblem = sqle.getSQLState().startsWith("08"); // 08xxx class of statecodes indicate connection problems
                if (connProblem) {
                    LOGGER.error("Connection problem encountered, retrying...", sqle);
                } else {
                    LOGGER.error("Non-connection problem encountered", sqle);
                    throw new IllegalStateException(sqle);
                }
            }
        }
    }
    
    /**
     * Retry block.
     */
    public interface RetryBlock {
        /**
         * Retry block.
         * @throws SQLException sql error
         */
        void run() throws SQLException;
    }
    

}
