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
import com.offbynull.actors.redisclient.Connection;
import com.offbynull.actors.redisclient.ConnectionException;
import static com.offbynull.actors.redisclient.RedisUtils.toClusterKey;
import com.offbynull.actors.redisclient.Transaction;
import com.offbynull.actors.redisclient.TransactionResult;
import com.offbynull.actors.redisclient.Watch;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * Actor accessor -- sets and accesses properties of a stored actor.
 * <p>
 * Unless there's a critical error, implementations are required to retry indefinity the operation until it succeeds. The following are not
 * considered to be critical errors...
 * <ul>
 * <li>Connection problems.</li>
 * <li>Redis MULTI/EXEC transactions that fail because a WATCH failed are not critical errors.</li>
 * </ul>
 * @author Kasra Faghihi
 */
final class ActorAccessor {

    private static final String KEY_PREFIX = "actor:";

    private static final String CHECKPOINT_MSG_KEY_SUFFIX = ":checkpointmsg";
    private static final String CHECKPOINT_DATA_KEY_SUFFIX = ":checkpointdata";
    private static final String CHECKPOINT_TIME_KEY_SUFFIX = ":checkpointtime";
    private static final String CHECKPOINT_INSTANCE_KEY_SUFFIX = ":checkpointinstance";
    private static final String DATA_KEY_SUFFIX = ":data";
    private static final String MSG_QUEUE_SUFFIX = ":msgqueue";
    private static final String STATE_KEY_SUFFIX = ":state";
    
    private static final String STATE_IDLE = "idle";
    private static final String STATE_PROCESSING = "processing";

    private final Connection connection;
    private final String checkpointMsgKey;
    private final String checkpointDataKey;
    private final String checkpointTimeKey;
    private final String checkpointInstanceKey;
    private final String dataKey;
    private final String msgQueueKey;
    private final String stateKey;

    ActorAccessor(Connection connection, Address address) {
        Validate.notNull(connection);
        Validate.notNull(address);

        this.connection = connection;

        this.checkpointMsgKey = toClusterKey(KEY_PREFIX, address, CHECKPOINT_MSG_KEY_SUFFIX);
        this.checkpointDataKey = toClusterKey(KEY_PREFIX, address, CHECKPOINT_DATA_KEY_SUFFIX);
        this.checkpointTimeKey = toClusterKey(KEY_PREFIX, address, CHECKPOINT_TIME_KEY_SUFFIX);
        this.checkpointInstanceKey = toClusterKey(KEY_PREFIX, address, CHECKPOINT_INSTANCE_KEY_SUFFIX);
        this.dataKey = toClusterKey(KEY_PREFIX, address, DATA_KEY_SUFFIX);
        this.msgQueueKey = toClusterKey(KEY_PREFIX, address, MSG_QUEUE_SUFFIX);
        this.stateKey = toClusterKey(KEY_PREFIX, address, STATE_KEY_SUFFIX);
    }

    boolean update(byte[] data, byte[] checkpointPayload, long checkpointTime, int checkpointInstance) throws ConnectionException {
        Validate.notNull(data);
        Validate.isTrue(
                !(checkpointPayload == null ^ checkpointTime < 0L),
                "If checkpoingPayload is set checkpointTime must be > 0 and vice versa");
        // checkpointPayload may be null if it was not set
        // checkpointInstance can be anything
        // checkpointTime if negative means that nothing should be checkpointed
        

        // Calculate the checkpoint we should have for the watch. Don't update/insert the actor unless the new checkpoint instance is
        // either not there, the same, or up by one.
        Integer storedCheckpointInstance = connection.get(checkpointInstanceKey, ConversionUtils::stringToInt);
        Integer expectedCheckpointInstance;
        if (storedCheckpointInstance == null) {                      // actor doesn't exist, we can put it in
            expectedCheckpointInstance = null;
        } else if (checkpointInstance == storedCheckpointInstance) { // actor exists + is at same checkpoint, we can update
            expectedCheckpointInstance = storedCheckpointInstance;
        } else {                                                     // actor exists but is at newer instance, ignore update
            return false;
        }

        TransactionResult res = connection.transaction(
                new Transaction(true, queue -> {
                    queue.set(checkpointInstanceKey, checkpointInstance);
                    queue.set(dataKey, data);
                    queue.set(stateKey, STATE_IDLE);
                    if (checkpointPayload != null) {
                        queue.set(checkpointDataKey, data);
                        queue.set(checkpointMsgKey, checkpointPayload);
                        queue.set(checkpointTimeKey, checkpointTime);
                    }
                }),
                // Before inserting, make sure the checkpoint instance in redis didn't change
                new Watch(checkpointInstanceKey, false, () -> {
                    Integer existingCheckpointInstance = connection.get(checkpointInstanceKey, ConversionUtils::stringToInt);
                    return Objects.equals(expectedCheckpointInstance, existingCheckpointInstance);
                }),
                // Make sure to watch all keys for this actor -- this is required for clustering because keys could be moving while the
                // transaction is happening. The idea with doing this is that, by watching all the keys (after the legitment watches),
                // it'll either hold off the server from moving the key or fail the MULTI/EXEC transaction while it is moving.
                new Watch(checkpointMsgKey, true, () -> true),
                new Watch(checkpointDataKey, true, () -> true),
                new Watch(checkpointTimeKey, true, () -> true),
                new Watch(checkpointInstanceKey, true, () -> true),
                new Watch(dataKey, true, () -> true),
                new Watch(msgQueueKey, true, () -> true),
                new Watch(stateKey, true, () -> true)
        );

        return res != null;
    }

    void putMessage(byte[] data) throws ConnectionException {
        Validate.notNull(data);

        connection.transaction(
                new Transaction(true, queue -> {
                    queue.lpush(msgQueueKey, data);
                }),
                new Watch(dataKey, false, () -> connection.exists(dataKey)),
                // Make sure to watch all keys for this actor -- this is required for clustering because keys could be moving while the
                // transaction is happening. The idea with doing this is that, by watching all the keys (after the legitment watches),
                // it'll either hold off the server from moving the key or fail the MULTI/EXEC transaction while it is moving.
                new Watch(checkpointMsgKey, true, () -> true),
                new Watch(checkpointDataKey, true, () -> true),
                new Watch(checkpointTimeKey, true, () -> true),
                new Watch(checkpointInstanceKey, true, () -> true),
                new Watch(dataKey, true, () -> true),
                new Watch(msgQueueKey, true, () -> true),
                new Watch(stateKey, true, () -> true)
        );
    }
    
    public Work nextMessage() throws ConnectionException {
        TransactionResult ret = connection.transaction(
                // if the multi/exec block fails, do retry (retries the whole thing, including the watches)
                new Transaction(true, queue -> {
                    queue.get(checkpointInstanceKey, ConversionUtils::stringToInt);
                    queue.rpop(msgQueueKey);
                    queue.get(dataKey);
                    queue.set(stateKey, STATE_PROCESSING);
                }),
                // Make sure message queue is not empty and the actor isn't processing a message (is idle)
                new Watch(msgQueueKey, false, () -> connection.llen(msgQueueKey) > 0L),
                new Watch(stateKey, false, () -> {
                    String state = connection.get(stateKey, ConversionUtils::byteArrayToString);
                    return STATE_IDLE.equals(state);
                }),
                // Make sure to watch all keys for this actor -- this is required for clustering because keys could be moving while the
                // transaction is happening. The idea with doing this is that, by watching all the keys (after the legitment watches),
                // it'll either hold off the server from moving the key or fail the MULTI/EXEC transaction while it is moving.
                new Watch(checkpointMsgKey, true, () -> true),
                new Watch(checkpointDataKey, true, () -> true),
                new Watch(checkpointTimeKey, true, () -> true),
                new Watch(checkpointInstanceKey, true, () -> true),
                new Watch(dataKey, true, () -> true),
                new Watch(msgQueueKey, true, () -> true),
                new Watch(stateKey, true, () -> true)
        );

        if (ret == null) {
            return null;
        }

        Validate.validState(ret.size() == 4); // sanity check

        return new Work(
                (byte[]) ret.get(2),
                (byte[]) ret.get(1),
                ((Integer) ret.get(0)));
    }
    

    public boolean isIdleAndHasMessages() throws ConnectionException {
        TransactionResult ret = connection.transaction(
                // if the multi/exec block fails, do retry (retries the whole thing, including the watches)
                new Transaction(true, queue -> {
                    queue.get(stateKey, ConversionUtils::byteArrayToString);
                    queue.exists(msgQueueKey); // msgQueueQueue won't exists if empty
                }),
                // Make sure to watch all keys for this actor -- this is required for clustering because keys could be moving while the
                // transaction is happening. The idea with doing this is that, by watching all the keys (after the legitment watches),
                // it'll either hold off the server from moving the key or fail the MULTI/EXEC transaction while it is moving.
                new Watch(checkpointMsgKey, true, () -> true),
                new Watch(checkpointDataKey, true, () -> true),
                new Watch(checkpointTimeKey, true, () -> true),
                new Watch(checkpointInstanceKey, true, () -> true),
                new Watch(dataKey, true, () -> true),
                new Watch(msgQueueKey, true, () -> true),
                new Watch(stateKey, true, () -> true)
        );

        Validate.validState(ret.size() == 2); // sanity check

        return STATE_IDLE.equals(ret.get(0)) && (boolean) ret.get(1);
    }
    
    public Work checkpointMessage(long currentTime) throws ConnectionException {
        Validate.isTrue(currentTime >= 0);

        TransactionResult ret = connection.transaction(
                // if the multi/exec block fails, do retry (retries the whole thing, including the watches)
                new Transaction(true, queue -> {
                    queue.incr(checkpointInstanceKey);             // increment checkpoint instance
                    queue.set(checkpointTimeKey, Long.MAX_VALUE);  // max out checkpoint time to max so we don't hit checkpoint again
                    queue.get(checkpointMsgKey);
                    queue.get(checkpointDataKey);
                    queue.set(stateKey, STATE_PROCESSING);
                }),
                // Make sure checkpointTime exists and it's greater than currentTime.
                new Watch(checkpointTimeKey, false, () -> {
                    Long checkpointTime = connection.get(checkpointTimeKey, ConversionUtils::stringToLong);
                    return checkpointTime != null && checkpointTime <= currentTime;
                }),
                // Make sure to watch all keys for this actor -- this is required for clustering because keys could be moving while the
                // transaction is happening. The idea with doing this is that, by watching all the keys (after the legitment watches),
                // it'll either hold off the server from moving the key or fail the MULTI/EXEC transaction while it is moving.
                new Watch(checkpointMsgKey, true, () -> true),
                new Watch(checkpointDataKey, true, () -> true),
                new Watch(checkpointTimeKey, true, () -> true),
                new Watch(checkpointInstanceKey, true, () -> true),
                new Watch(dataKey, true, () -> true),
                new Watch(msgQueueKey, true, () -> true),
                new Watch(stateKey, true, () -> true)
        );

        if (ret == null) {
            return null;
        }

        Validate.validState(ret.size() == 5); // sanity check
        return new Work(
                (byte[]) ret.get(3),
                (byte[]) ret.get(2),
                ((Long) ret.get(0)).intValue()
        );
    }
    
    public void remove() throws ConnectionException {
        connection.transaction(
                new Transaction(true, queue -> {
                    queue.del(checkpointMsgKey);
                    queue.del(checkpointDataKey);
                    queue.del(checkpointTimeKey);
                    queue.del(checkpointInstanceKey);
                    queue.del(dataKey);
                    queue.del(msgQueueKey);
                    queue.del(stateKey);
                }),
                // Make sure to watch all keys for this actor -- this is required for clustering because keys could be moving while the
                // transaction is happening. The idea with doing this is that, by watching all the keys (after the legitment watches),
                // it'll either hold off the server from moving the key or fail the MULTI/EXEC transaction while it is moving.
                new Watch(checkpointMsgKey, true, () -> true),
                new Watch(checkpointTimeKey, true, () -> true),
                new Watch(checkpointInstanceKey, true, () -> true),
                new Watch(dataKey, true, () -> true),
                new Watch(msgQueueKey, true, () -> true),
                new Watch(stateKey, true, () -> true)
        );
    }
    
    
    
    
    
    
    
    
    
    
    
    static final class Work {
        private final byte[] actorData;
        private final byte[] messageData;
        private final int checkpointInstance;

        Work(byte[] actorData, byte[] messageData, int checkpointInstance) {
            Validate.notNull(actorData);
            Validate.notNull(messageData);

            this.actorData = actorData.clone();
            this.messageData = messageData.clone();
            this.checkpointInstance = checkpointInstance;
        }

        byte[] getActorData() {
            return actorData.clone();
        }

        byte[] getMessageData() {
            return messageData.clone();
        }

        int getCheckpointInstance() {
            return checkpointInstance;
        }
    }
}
