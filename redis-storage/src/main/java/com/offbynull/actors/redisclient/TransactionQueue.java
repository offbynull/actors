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
package com.offbynull.actors.redisclient;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * Redis operations to queue up inside a MULTI/EXEC block.
 * @author Kasra Faghihi
 */
public interface TransactionQueue {

    /**
     * Queue up a redis ZRANGE operation.
     * @param key redis key
     * @param start start rank (index)
     * @param end end rank (index)
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    default void zrange(String key, long start, long end) throws ConnectionException {
        zrange(key, start, end, v -> v);
    }
    
    /**
     * Queue up a redis ZRANGE operation.
     * @param <T> expected type
     * @param key redis key
     * @param start start rank (index)
     * @param end end rank (index)
     * @param converter value converter (converts raw value to expected type)
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null} 
     * @throws IllegalStateException if closed
     */
    <T> void zrange(String key, long start, long end, Function<byte[], T> converter) throws ConnectionException;

    /**
     * Queue up a redis ZREMRANGEBYRANK operation.
     * @param key redis key
     * @param start start rank (index)
     * @param end end rank (index)
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    void zremrangeByRank(String key, long start, long end) throws ConnectionException;

    /**
     * Queue up a redis LPUSH operation.
     * @param key redis key
     * @param val value
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null} 
     * @throws IllegalStateException if closed
     */
    void lpush(String key, byte[] val) throws ConnectionException;

    /**
     * Queue up a redis RPOP operation.
     * @param key redis key
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null} 
     * @throws IllegalStateException if closed
     */
    default void rpop(String key) throws ConnectionException {
        rpop(key, v -> v);
    }

    /**
     * Queue up a redis RPOP operation.
     * @param <T> expected type
     * @param key redis key
     * @param converter value converter (converts raw value to expected type)
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    <T> void rpop(String key, Function<byte[], T> converter) throws ConnectionException;

    /**
     * Queue up a redis SET operation.
     * @param key redis key
     * @param val value
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null} 
     * @throws IllegalStateException if closed
     */
    default void set(String key, int val) throws ConnectionException {
        set(key, "" + val);
    }
    
    /**
     * Queue up a redis SET operation.
     * @param key redis key
     * @param val value
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    default void set(String key, long val) throws ConnectionException {
        set(key, "" + val);
    }

    /**
     * Queue up a redis SET operation.
     * @param key redis key
     * @param val value
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    default void set(String key, String val) throws ConnectionException {
        set(key, val.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Queue up a redis SET operation.
     * @param key redis key
     * @param val value
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    void set(String key, byte[] val) throws ConnectionException;

    /**
     * Queue up a redis GET operation.
     * @param key redis key
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    default void get(String key) throws ConnectionException {
        get(key, v -> v);
    }
    
    /**
     * Queue up a redis GET operation.
     * @param <T> expected type
     * @param key redis key
     * @param converter value converter (converts raw value to expected type)
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    <T> void get(String key, Function<byte[], T> converter) throws ConnectionException;
    
    /**
     * Queue up a redis EXISTS operation.
     * @param key redis key
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    void exists(String key) throws ConnectionException;
    
    /**
     * Queue up a redis INCR operation.
     * @param key redis key
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    void incr(String key) throws ConnectionException;

    /**
     * Queue up a redis DEL operation.
     * @param key redis key
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    void del(String key) throws ConnectionException;
}
