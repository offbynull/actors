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

final class InQueue {
    private final Connection connection;
    private final QueueDetails queueDetails;

    InQueue(Connection connection, QueueDetails queueDetails) {
        Validate.notNull(connection);
        Validate.notNull(queueDetails);

        this.connection = connection;
        this.queueDetails = queueDetails;
    }

    void queueIn(String id, int offset, List<byte[]> messages) throws ConnectionException {
        Validate.notNull(id);
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        Validate.isTrue(offset >= 0);

        String inQueueKey = queueDetails.getInQueueKey();
        String inQueueOffsetKey = queueDetails.getInQueueOffsetKey();
        String outQueueKey = queueDetails.getOutQueueKey();
        String outQueueOffsetKey = queueDetails.getOutQueueOffsetKey();
        long expireTimestamp = queueDetails.getTimeout() + System.currentTimeMillis();
        boolean messageAdded = false;
        while (true) {
            // Get in transaction -- transaction will auto retry until consistent values are returned
            TransactionResult res1 = connection.transaction(
                    new Transaction(true, queue -> {
                        queue.llen(inQueueKey);
                        queue.get(inQueueOffsetKey, ConversionUtils::stringToInt);
                    }),
                    new Watch(inQueueKey, true, () -> true),
                    new Watch(inQueueOffsetKey, true, () -> true)
            );
            long queueLen = res1.get(0);
            int queueOffset = res1.get(1) == null ? 0 : res1.get(1);


            // if > last element, this method will fail.
            // if <= first element, this method will silently ignore everything up to and including the first element.                
            int tailOffset = queueOffset + (int) queueLen;
            int tailLen = messages.size() - (tailOffset - offset);

            if (tailLen < 0) {
                return;
            }

            if (tailLen > messages.size()) {
                throw new IllegalStateException();
            }

            // we can me sure at this point that the offset to be written to is exactly at the end of the queue...



            // Append in transaction -- if queue or queueoffset changes, the transaction fails and it will MANUALLY retry by executing
            // the entire loop again
            int msgCropStart = messages.size() - tailLen;
            int msgCropEnd = messages.size();
            List<byte[]> messagesToAppend = messages.subList(msgCropStart, msgCropEnd);
            TransactionResult res2 = connection.transaction(
                    new Transaction(false, queue -> {
                        for (byte[] message : messagesToAppend) {
                            queue.rpush(inQueueKey, message);
                        }
                        queue.set(inQueueOffsetKey, queueOffset); // may not actually have been set, so force set it
                    }),
                    new Watch(inQueueKey, false, () -> connection.llen(inQueueKey) == queueLen),
                    new Watch(inQueueOffsetKey, false, () -> {
                        Integer updatedQueueOffset = connection.get(inQueueOffsetKey, ConversionUtils::stringToInt);
                        if (updatedQueueOffset == null) {
                            updatedQueueOffset = 0;
                        }
                        return queueOffset == updatedQueueOffset;
                    })
            );
            offset++;

            if (res2 != null) {
                messageAdded = true;
                break;
            }
        }

        // Set ALL queues to evict at the same time -- note that this only happens if a message was inserted. The Redis implementation
        // will evict queues if the client implementation doesn't periodically pump messages into the system.
        if (messageAdded) {
            connection.pexireAt(outQueueKey, expireTimestamp);
            connection.pexireAt(outQueueOffsetKey, expireTimestamp);
            connection.pexireAt(inQueueKey, expireTimestamp);
            connection.pexireAt(inQueueOffsetKey, expireTimestamp);
        }
    }

    List<byte[]> dequeueIn(String id) throws ConnectionException {
        Validate.notNull(id);

        String inQueueKey = queueDetails.getInQueueKey();
        String inQueueOffsetKey = queueDetails.getInQueueOffsetKey();
        while (true) {
            // Get in transaction -- transaction will auto retry until consistent values are returned
            TransactionResult res1 = connection.transaction(
                    new Transaction(true, queue -> {
                        queue.llen(inQueueKey);
                        queue.get(inQueueOffsetKey, ConversionUtils::stringToInt);
                    }),
                    new Watch(inQueueKey, true, () -> true),
                    new Watch(inQueueOffsetKey, true, () -> true)
            );
            long queueLen = res1.get(0);
            int queueOffset = res1.get(1) == null ? 0 : res1.get(1);
            
            if (queueLen == 0) {
                return new LinkedList<>();
            }

            // Empty queue in transaction -- if queue or queueoffset changes, the transaction fails and it will MANUALLY retry by executing
            // the entire loop again
            TransactionResult res2 = connection.transaction(
                    new Transaction(false, queue -> {
                        for (int i = 0; i < queueLen; i++) {
                            queue.lpop(inQueueKey);
                        }
                        queue.set(inQueueOffsetKey, queueOffset + queueLen); // update offset
                    }),
                    new Watch(inQueueKey, false, () -> connection.llen(inQueueKey) == queueLen),
                    new Watch(inQueueOffsetKey, false, () -> {
                        Integer updatedQueueOffset = connection.get(inQueueOffsetKey, ConversionUtils::stringToInt);
                        if (updatedQueueOffset == null) {
                            updatedQueueOffset = 0;
                        }
                        return queueOffset == updatedQueueOffset;
                    })
            );

            if (res2 != null) {
                LinkedList<byte[]> ret = new LinkedList<>();
                for (int i = 0; i < queueLen; i++) {
                    ret.add(res2.get(i));
                }
                return ret;
            }
        }
    }
}
