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

import com.offbynull.actors.redisclient.Connection;
import com.offbynull.actors.redisclient.ConnectionException;
import com.offbynull.actors.redisclient.Transaction;
import com.offbynull.actors.redisclient.TransactionResult;
import com.offbynull.actors.redisclient.Watch;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class OutQueue {
    private final Connection connection;
    private final QueueDetails queueDetails;

    OutQueue(Connection connection, QueueDetails queueDetails) {
        Validate.notNull(connection);
        Validate.notNull(queueDetails);

        this.connection = connection;
        this.queueDetails = queueDetails;
    }
    
    
    
    
    
    void queueOut(String id, List<byte[]> messages) throws ConnectionException {
        Validate.notNull(id);
        Validate.notNull(messages);
        Validate.noNullElements(messages);

        String outQueueKey = queueDetails.getOutQueueKey();
        String outQueueOffsetKey = queueDetails.getOutQueueOffsetKey();
        while (true) {
            // Get in transaction -- transaction will auto retry until consistent values are returned
            TransactionResult res1 = connection.transaction(
                    new Transaction(true, queue -> {
                        queue.llen(outQueueKey);
                        queue.get(outQueueOffsetKey, ConversionUtils::stringToInt);
                    }),
                    new Watch(outQueueKey, true, () -> true),
                    new Watch(outQueueOffsetKey, true, () -> true)
            );
            long queueLen = res1.get(0);
            int queueOffset = res1.get(1) == null ? 0 : res1.get(1);

            // Append to queue in transaction -- if queue or queueoffset changes, the transaction fails and it will MANUALLY retry by
            // executing the entire loop again
            TransactionResult res2 = connection.transaction(
                    new Transaction(false, queue -> {
                        for (byte[] message : messages) {
                            queue.rpush(outQueueKey, message);
                        }
                    }),
                    new Watch(outQueueKey, false, () -> connection.llen(outQueueKey) == queueLen),
                    new Watch(outQueueOffsetKey, false, () -> {
                        Integer updatedQueueOffset = connection.get(outQueueOffsetKey, ConversionUtils::stringToInt);
                        if (updatedQueueOffset == null) {
                            updatedQueueOffset = 0;
                        }
                        return queueOffset == updatedQueueOffset;
                    })
            );

            if (res2 != null) {
                break;
            }
        }        
    }

    List<byte[]> dequeueOut(String id, int offset) throws ConnectionException {
        Validate.notNull(id);
        Validate.isTrue(offset >= 0);

        String outQueueKey = queueDetails.getOutQueueKey();
        String outQueueOffsetKey = queueDetails.getOutQueueOffsetKey();
        while (true) {
            // Get in transaction -- transaction will auto retry until consistent values are returned
            TransactionResult res1 = connection.transaction(
                    new Transaction(true, queue -> {
                        queue.llen(outQueueKey);
                        queue.get(outQueueOffsetKey, ConversionUtils::stringToInt);
                    }),
                    new Watch(outQueueKey, true, () -> true),
                    new Watch(outQueueOffsetKey, true, () -> true)
            );
            long queueLen = res1.get(0);
            int queueOffset = res1.get(1) == null ? 0 : res1.get(1);



            // if == last element + 1, this method will return an empty list.
            if (offset == queueOffset + queueLen) {
                return new LinkedList<>();
            }

            // if > last element + 1, this method will fail.
            if (offset > queueOffset + queueLen) {
                throw new IllegalStateException();
            }

            // if < first element, this method will fails.
            if (offset < queueOffset) {
                throw new IllegalStateException();
            }

            // we can me sure at this point that the offset to be written to is exactly at the end of the queue...



            // Append in transaction -- if queue or queueoffset changes, the transaction fails and it will MANUALLY retry by executing
            // the entire loop again
            TransactionResult res2 = connection.transaction(
                    new Transaction(false, queue -> {
                        int count = offset - queueOffset;
                        for (int i = queueOffset; i < offset; i++) {
                            queue.lpop(outQueueKey);
                        }
                        queue.set(outQueueOffsetKey, offset);
                        queue.lrange(outQueueKey, 0, Integer.MAX_VALUE);
                    }),
                    new Watch(outQueueKey, false, () -> connection.llen(outQueueKey) == queueLen),
                    new Watch(outQueueOffsetKey, false, () -> {
                        Integer updatedQueueOffset = connection.get(outQueueOffsetKey, ConversionUtils::stringToInt);
                        if (updatedQueueOffset == null) {
                            updatedQueueOffset = 0;
                        }
                        return queueOffset == updatedQueueOffset;
                    })
            );

            if (res2 != null) {
                List<byte[]> lrangeRes = res2.get(res2.size() - 1);
                return new LinkedList<>(lrangeRes);
            }
        }
    }
}
