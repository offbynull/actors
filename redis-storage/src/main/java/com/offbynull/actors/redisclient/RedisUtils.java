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

import com.offbynull.actors.shuttle.Address;
import java.io.IOException;
import static java.util.Collections.singletonMap;
import org.apache.commons.lang3.Validate;
import org.apache.commons.text.translate.AggregateTranslator;
import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.LookupTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis client utilities.
 * @author Kasra Faghihi
 */
public final class RedisUtils {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisUtils.class);
    
    private static final CharSequenceTranslator BRACE_REMOVAL_TRANSLATOR = new AggregateTranslator(
            new LookupTranslator(singletonMap("{", "\\u007B")),
            new LookupTranslator(singletonMap("}", "\\u007D")),
            new LookupTranslator(singletonMap("\\", "\\\\"))
    );
    
    private RedisUtils() {
        // do nothing
    }

    /**
     * Create a Redis key that forces keys with the same address onto the same Redis cluster node (via Redis hash tags).
     * @param prefix key prefix
     * @param address address for key (this is where hashtag is applied)
     * @param suffix key suffix
     * @return hash-tagged key 
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if either {@code prefix} or {@code suffix} contains curly braces -- curly braces are reserved for
     * Redis hash tags.
     */
    public static String toClusterKey(String prefix, Address address, String suffix) {
        Validate.notNull(prefix);
        Validate.notNull(address);
        Validate.notNull(suffix);
        Validate.isTrue(!prefix.contains("{") && !prefix.contains("}"));
        Validate.isTrue(!suffix.contains("{") && !suffix.contains("}"));
        // If we're operating on a Redis cluster, we need to force all the keys associated with a actor to sit on the same node. We need to
        // do this because we use watch/multi/exec transactions to ensure data consistency for the actors we store.
        //
        // The way to force all the keys associated with a actor to sit on the same node is to use Redis hash tags. A Redis hash tag is the
        // part of the key wrapped in { and } braces -- only the stuff between the braces will be hashed to figure out what node the key
        // should point to.
        //
        // We have multiple keys for an actor, but the address of each actor will be in every one of those keys. As such, we wrap the
        // address part of the key in { and } braces to force all those keys onto the same node.
        
        
        // Remove any braces from the address because they'll mess with the braces we put in for the hash tag -- Redis doesn't seem to
        // provide a way to escape braces.
        //
        // This translates { and } to their Java string-escaped unicode equivalents, and translates \ to \\. So for example...
        //   {abc}                             will become                    \u007Babc\u007D
        //   \u007B{abc}\u007D                 will become                    \\u007B\u007Babc\u007D\\u007D
        //
        // There is no chance for conflict here.
        String addrStr = BRACE_REMOVAL_TRANSLATOR.translate(address.toString());
        
        return prefix + '{' + addrStr + '}' + suffix;
    }
    
    
    
    
    /**
     * Retry a Redis operation.
     * @param <V> return type
     * @param block retry block
     * @return return value
     */
    public static <V> V retry(RetryReturnBlock<V> block) {
        while (true) {
            try {
                return block.run();
            } catch (ConnectionException e) {
                if (e.isConnectionProblem()) {
                    LOGGER.error("Connection problem encountered, retrying...", e);
                } else {
                    LOGGER.error("Non-connection problem encountered", e);
                    throw new IllegalStateException(e);
                }
            } catch (IOException e) {
                LOGGER.error("IO problem encountered, retrying...", e);
            } catch (RuntimeException e) {
                LOGGER.error("Non-connection problem encountered", e);
                throw new IllegalStateException(e);
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
         * @throws IOException io error
         * @throws ConnectionException connection error
         */
        V run() throws IOException, ConnectionException;
    }

    /**
     * Retry a Redis operation.
     * @param block retry block
     */
    public static void retry(RetryBlock block) {
        while (true) {
            try {
                block.run();
                return;
            } catch (ConnectionException e) {
                if (e.isConnectionProblem()) {
                    LOGGER.error("Connection problem encountered, retrying...", e);
                } else {
                    LOGGER.error("Non-connection problem encountered", e);
                    throw new IllegalStateException(e);
                }
            } catch (IOException e) {
                LOGGER.error("IO problem encountered, retrying...", e);
            } catch (RuntimeException e) {
                LOGGER.error("Non-connection problem encountered", e);
                throw new IllegalStateException(e);
            }
        }
    }
    
    /**
     * Retry block.
     */
    public interface RetryBlock {
        /**
         * Retry block.
         * @throws IOException io error
         * @throws ConnectionException connection error
         */
        void run() throws IOException, ConnectionException;
    }
}
