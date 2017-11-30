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
package com.offbynull.actors.stores.redis.connector;

import java.io.Closeable;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Collection;
import java.util.function.Function;

/**
 * A low-level Redis connection.
 * <p>
 * Implementations may be bound to a {@link Connector}. That means that if the {@link Connector} that generated this {@link Connection} is
 * closed, this {@link Connection} may also be closed.
 * <p>
 * Implementations may or may not be thread-safe.
 * @author Kasra Faghihi
 */
public interface Connection extends Closeable {

    /**
     * Redis EXISTS operation.
     * @param key redis key
     * @return {@code true} if the key exists, {@code false} otherwise
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    boolean exists(String key) throws ConnectionException;

    /**
     * Redis GET operation.
     * @param key redis key
     * @return raw value for the key (or {@code null} if does not exist)
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    default byte[] get(String key) throws ConnectionException {
        return get(key, v -> v);
    }

    /**
     * Redis GET operation.
     * @param key redis key
     * @param converter value converter (converts raw value to expected type)
     * @param <T> expected value type
     * @return value for the key (or {@code null} if does not exist)
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    <T> T get(String key, Function<byte[], T> converter) throws ConnectionException;

    /**
     * Redis LLEN operation.
     * @param key redis key
     * @return length of the list in the key
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    long llen(String key) throws ConnectionException;

    /**
     * Redis ZADD operation.
     * @param key redis key
     * @param score for the value being added
     * @param val string value (written as UTF-8)
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    default void zadd(String key, double score, String val) throws ConnectionException {
        zadd(key, score, val.getBytes(UTF_8));
    }
    
    /**
     * Redis ZADD operation.
     * @param key redis key
     * @param score for the value being added
     * @param val raw value being added
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    void zadd(String key, double score, byte[] val) throws ConnectionException;

    /**
     * Redis ZRANGE WITHSCORES operation.
     * @param key redis key
     * @param start start rank (index)
     * @param end end rank (index)
     * @param converter value converter (converts raw value of items to expected type)
     * @param <T> expected value type
     * @return values for the specified range
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    <T> Collection<SortedSetItem> zrangeWithScores(String key, long start, long end, Function<byte[], T> converter)
            throws ConnectionException;
    
    /**
     * Perform a Redis transaction, with watches.
     * <p>
     * This is a automated/hardened way of doing Redis WATCH/MULTI/EXEC transactions. It works by checking conditions before and after your
     * WATCHes, such that you know if your anything changed while you were issuing your WATCHes. The rules are ...
     * <ol>
     * <li>Check conditions</li>
     * <li>Execute WATCH command</li>
     * <li>Check conditions again (to make sure nothing changed while WATCHes were being set)</li>
     * <li>Execute MULTI command</li>
     * <li>Execute commands to queue for transaction</li>
     * <li>Execute EXEC command</li>
     * </ol>
     * @param transaction transaction block (where commands between MULTI and EXEC are queued)
     * @param watches watch blocks (keys to watch and conditions to check for each key)
     * @return transaction results
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    TransactionResult transaction(Transaction transaction, Watch... watches) throws ConnectionException;
}
