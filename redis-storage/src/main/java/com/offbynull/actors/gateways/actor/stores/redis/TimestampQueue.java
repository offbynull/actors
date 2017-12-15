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
package com.offbynull.actors.gateways.actor.stores.redis;

import com.offbynull.actors.address.Address;
import org.apache.commons.lang3.Validate;
import java.util.Collection;
import com.offbynull.actors.redisclient.Connection;
import com.offbynull.actors.redisclient.ConnectionException;
import com.offbynull.actors.redisclient.SortedSetItem;
import com.offbynull.actors.redisclient.Transaction;
import com.offbynull.actors.redisclient.TransactionResult;
import com.offbynull.actors.redisclient.Watch;

/**
 * Timestamp queue -- a queue of (timestamp, address) pairs that's sorted in ascending order by timestamp.
 * <p>
 * Unless there's a critical error, implementations are required to retry indefinity the operation until it succeeds. The following are not
 * considered to be critical errors...
 * <ul>
 * <li>Connection problems.</li>
 * <li>Redis MULTI/EXEC transactions that fail because a WATCH failed are not critical errors.</li>
 * </ul>
 * @author Kasra Faghihi
 */
final class TimestampQueue {
    private static final String KEY_PREFIX = "timestampqueue:";

    private final Connection connection;
    private final String queueKey;

    TimestampQueue(Connection connection, String name, int num) {
        Validate.notNull(name);
        Validate.notNull(connection);

        this.queueKey = KEY_PREFIX + name + ':' + num;
        this.connection = connection;
    }
    
    // remove next item (only if the timestamp for the next item <= currentTimestamp)
    public Address remove(long minTimestamp) throws ConnectionException {
        Validate.isTrue(minTimestamp >= 0L);

        TransactionResult res = connection.transaction(
                new Transaction(true, queue -> {
                    queue.zrange(queueKey, 0L, 0L, ConversionUtils::byteArrayToString);
                    queue.zremrangeByRank(queueKey, 0L, 0L);
                }),
                new Watch(queueKey, false, () -> {
                    long timestamp = peekTimestamp();
                    return timestamp != -1 && timestamp < minTimestamp;
                })
        );

        if (res == null) {
            return null;
        }

        Validate.validState(res.size() == 2); // sanity check (2 commands executed in transaction)

        Collection<String> zrangeResult = res.get(0);
        if (zrangeResult != null && !zrangeResult.isEmpty()) {
            String addrStr = zrangeResult.iterator().next();
            return Address.fromString(addrStr);
        }

        return null;
    }

    // insert a new address into the queue
    public void insert(long timestamp, Address address) throws ConnectionException {
        Validate.notNull(address);
        Validate.isTrue(timestamp >= 0L);
        
        // Because this is a sorted set, we need to have unique addresses going into the set. The problem is that we can have multiple
        // checks queued up for an address. The way we work around this is by appending some garbage to the end of the address to make it
        // unique -- the timestamp in this case.
        String addrStr = address.toString();
        connection.zadd(queueKey, timestamp, addrStr);

        // inspectionTimestamp is a long, but it's getting converted to a double. Does this mean there's a loss in precision? According
        // to the following stackoverflow post, we can be reasonably sure that loss of *integer* precision won't be an issue (we don't
        // care about the fractional portion): https://stackoverflow.com/a/873367/1196226
        //
        //
        // Around 2^53 is where you start losing integer precision. If you plug this into a DateTime object to see around what year this
        // is, it'll come out to 287396...
        //
        // long precisionLossTime = 1L << 53L;
        // Instant i = Instant.ofEpochMilli(precisionLossTime);
        // ZonedDateTime zdt = ZonedDateTime.ofInstant(i, ZoneId.systemDefault());
        // System.out.println(zdt);
        //
        // +287396-10-12T01:59:00.992-07:00[America/Los_Angeles]
        //
        //
        // Tests on redis-cli confirm this (tested on 64-bit Redis). 2^53 = 9007199254740992... when we use that as the score for a
        // member in a sorted set and get it back out, we get back the exact same score...
        // 127.0.0.1:6379> zadd ztest  9007199254740992 "a"
        // (integer) 1
        // 127.0.0.1:6379> zrange ztest 0 1000 WITHSCORES
        // 1) "a"
        // 2) "9007199254740992"
        //
        // When we use a larger number, we get back a score that has precision loss in the integer portion....
        // 127.0.0.1:6379> zadd ztest  99999007199254740992 "b"
        // (integer) 1
        // 127.0.0.1:6379> zrange ztest 0 1000 WITHSCORES
        // 1) "b"
        // 2) "9.9999007199254741e+19"
        //
        //
        // We can safely ignore the precision loss from the conversion to a double -- the year 287396 is too far off to care about.
    }
    
    // peek the timestamp of the next item in the inspection queue
    private long peekTimestamp() throws ConnectionException {
        Collection<SortedSetItem> result = connection.zrangeWithScores(queueKey, 0L, 0L, ConversionUtils::byteArrayToString);
        if (result.isEmpty()) {
            return -1;
        }

        return (long) result.iterator().next().getScore(); // score is the time
    }
}
