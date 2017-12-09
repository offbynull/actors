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
package com.offbynull.actors.gateways.servlet.stores.redis;

import com.offbynull.actors.address.Address;
import com.offbynull.actors.redisclient.Connection;
import com.offbynull.actors.redisclient.ConnectionException;
import static com.offbynull.actors.redisclient.RedisUtils.toClusterKey;
import com.offbynull.actors.redisclient.Transaction;
import com.offbynull.actors.redisclient.TransactionResult;
import com.offbynull.actors.redisclient.Watch;
import org.apache.commons.lang3.Validate;

/**
 * Message queue -- writes and reads HTTP client message queues.
 * <p>
 * Unless there's a critical error, implementations are required to retry indefinity the operation until it succeeds. The following are not
 * considered to be critical errors...
 * <ul>
 * <li>Connection problems.</li>
 * <li>Redis MULTI/EXEC transactions that fail because a WATCH failed are not critical errors.</li>
 * </ul>
 * @author Kasra Faghihi
 */
final class MessageQueue {

    private static final String KEY_PREFIX = "servlet:";

    private static final String MSG_QUEUE_SUFFIX = ":msgqueue";

    private final Connection connection;
    private final String msgQueueKey;
    private final long timeout;

    MessageQueue(Connection connection, Address address, long timeout) {
        Validate.notNull(connection);
        Validate.notNull(address);
        Validate.isTrue(timeout >= 0L);

        this.connection = connection;
        this.msgQueueKey = toClusterKey(KEY_PREFIX, address, MSG_QUEUE_SUFFIX);
        this.timeout = timeout;
    }

    void putMessage(byte[] data) throws ConnectionException {
        Validate.notNull(data);

        // why is this wrapped in a WATCH/MULTI/EXEC transaction? we don't want the key to MOVE if this is a cluster and if it is moving we
        // want to force it to retry (it is assumed watch will fail in this case)
        connection.transaction(
                new Transaction(true, queue -> {
                    queue.lpush(msgQueueKey, data);
                }),
                new Watch(msgQueueKey, true, () -> true)
        );
        connection.pexire(msgQueueKey, timeout);
    }

    byte[] take() throws ConnectionException {
        // why is this wrapped in a WATCH/MULTI/EXEC transaction? we don't want the key to MOVE if this is a cluster and if it is moving we
        // want to force it to retry (it is assumed watch will fail in this case)
        long len = connection.llen(msgQueueKey);
        Validate.validState(len <= Integer.MAX_VALUE); // should never happen (hopefully)

        TransactionResult ret = connection.transaction(
                new Transaction(true, queue -> {
                    queue.rpop(msgQueueKey);
                }),
                new Watch(msgQueueKey, false, () -> connection.llen(msgQueueKey) > 0L),
                new Watch(msgQueueKey, true, () -> true)
        );
        
        if (ret == null) {
            return null;
        }

        Validate.validState(ret.size() == 1); // sanity check

        return ret.get(0);
    }
}
