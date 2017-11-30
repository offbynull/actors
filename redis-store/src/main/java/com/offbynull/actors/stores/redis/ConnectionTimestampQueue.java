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

import com.offbynull.actors.shuttle.Address;
import org.apache.commons.lang3.Validate;
import com.offbynull.actors.stores.redis.client.ClientException;
import java.util.Collection;
import com.offbynull.actors.stores.redis.client.TimestampQueue;
import com.offbynull.actors.stores.redis.connector.Connection;
import com.offbynull.actors.stores.redis.connector.ConnectionException;
import com.offbynull.actors.stores.redis.connector.SortedSetItem;
import com.offbynull.actors.stores.redis.connector.Transaction;
import com.offbynull.actors.stores.redis.connector.TransactionResult;
import com.offbynull.actors.stores.redis.connector.Watch;


final class ConnectionTimestampQueue implements TimestampQueue {
    private static final String KEY_PREFIX = "timestampqueue:";

    private final Connection client;
    private final String queueKey;

    ConnectionTimestampQueue(Connection client, String name, int num) {
        Validate.notNull(name);
        Validate.notNull(client);

        this.queueKey = KEY_PREFIX + name + ':' + num;
        this.client = client;
    }
    
    // remove next item (only if the timestamp for the next item <= currentTimestamp)
    @Override
    public Address remove(long minTimestamp) throws ClientException {
        Validate.isTrue(minTimestamp >= 0L);
        try {
            TransactionResult res = client.transaction(
                    new Transaction(true, queue -> {
                        queue.zrange(queueKey, 0L, 0L, InternalUtils::byteArrayToString);
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
        } catch (ConnectionException ce) {
            throw new ClientException(ce.isConnectionProblem(), ce);
        } catch (RuntimeException re) {
            throw new ClientException(false, re);
        }
    }

    // insert a new address into the queue
    @Override
    public void insert(long timestamp, Address address) throws ClientException {
        Validate.notNull(address);
        Validate.isTrue(timestamp >= 0L);
        // Because this is a sorted set, we need to have unique addresses going into the set. The problem is that we can have multiple
        // checks queued up for an address. The way we work around this is by appending some garbage to the end of the address to make it
        // unique -- the timestamp in this case.
        try {
            String addrStr = address.toString();
            client.zadd(queueKey, timestamp, addrStr);

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
        } catch (ConnectionException ce) {
            throw new ClientException(ce.isConnectionProblem(), ce);
        } catch (RuntimeException re) {
            throw new ClientException(false, re);
        }
    }
    
    // peek the timestamp of the next item in the inspection queue
    private long peekTimestamp() throws ConnectionException {
        Collection<SortedSetItem> result = client.zrangeWithScores(queueKey, 0L, 0L, InternalUtils::byteArrayToString);
        if (result.isEmpty()) {
            return -1;
        }

        return (long) result.iterator().next().getScore(); // score is the time
    }
}
