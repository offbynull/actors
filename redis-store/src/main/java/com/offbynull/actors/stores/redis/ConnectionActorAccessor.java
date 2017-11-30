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
import com.offbynull.actors.stores.redis.client.ActorAccessor;
import com.offbynull.actors.stores.redis.client.ClientException;
import com.offbynull.actors.stores.redis.connector.Connection;
import com.offbynull.actors.stores.redis.connector.ConnectionException;
import com.offbynull.actors.stores.redis.connector.Transaction;
import com.offbynull.actors.stores.redis.connector.TransactionResult;
import com.offbynull.actors.stores.redis.connector.Watch;
import static java.util.Collections.singletonMap;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.apache.commons.text.translate.AggregateTranslator;
import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.LookupTranslator;

final class ConnectionActorAccessor implements ActorAccessor {

    private static final String KEY_PREFIX = "actor:";

    private static final String CHECKPOINT_MSG_KEY_SUFFIX = ":checkpointmsg";
    private static final String CHECKPOINT_TIME_KEY_SUFFIX = ":checkpointtime";
    private static final String CHECKPOINT_INSTANCE_KEY_SUFFIX = ":checkpointinstance";
    private static final String DATA_KEY_SUFFIX = ":data";
    private static final String MSG_QUEUE_SUFFIX = ":msgqueue";
    private static final String STATE_KEY_SUFFIX = ":state";
    
    private static final String STATE_IDLE = "idle";
    private static final String STATE_PROCESSING = "processing";

    private final Connection client;
    private final String checkpointMsgKey;
    private final String checkpointTimeKey;
    private final String checkpointInstanceKey;
    private final String dataKey;
    private final String msgQueueKey;
    private final String stateKey;

    ConnectionActorAccessor(Connection client, Address address) {
        Validate.notNull(client);
        Validate.notNull(address);

        this.client = client;

        this.checkpointMsgKey = toClusterKey(KEY_PREFIX, address, CHECKPOINT_MSG_KEY_SUFFIX);
        this.checkpointTimeKey = toClusterKey(KEY_PREFIX, address, CHECKPOINT_TIME_KEY_SUFFIX);
        this.checkpointInstanceKey = toClusterKey(KEY_PREFIX, address, CHECKPOINT_INSTANCE_KEY_SUFFIX);
        this.dataKey = toClusterKey(KEY_PREFIX, address, DATA_KEY_SUFFIX);
        this.msgQueueKey = toClusterKey(KEY_PREFIX, address, MSG_QUEUE_SUFFIX);
        this.stateKey = toClusterKey(KEY_PREFIX, address, STATE_KEY_SUFFIX);
    }

    @Override
    public boolean update(byte[] data, byte[] checkpointPayload, long checkpointTime, int checkpointInstance) throws ClientException {
        Validate.notNull(data);
        Validate.isTrue(
                !(checkpointPayload == null ^ checkpointTime < 0L),
                "If checkpoingPayload is set checkpointTime must be > 0 and vice versa");
        // checkpointPayload may be null if it was not set
        // checkpointInstance can be anything
        // checkpointTime if negative means that nothing should be checkpointed
        
        try {
            // Calculate the checkpoint we should have for the watch. Don't update/insert the actor unless the new checkpoint instance is
            // either not there, the same, or up by one.
            Integer storedCheckpointInstance = client.get(checkpointInstanceKey, InternalUtils::stringToInt);
            Integer expectedCheckpointInstance;
            if (storedCheckpointInstance == null) {                      // actor doesn't exist, we can put it in
                expectedCheckpointInstance = null;
            } else if (checkpointInstance == storedCheckpointInstance) { // actor exists + is at same checkpoint, we can update
                expectedCheckpointInstance = storedCheckpointInstance;
            } else if (checkpointInstance > storedCheckpointInstance) {  // actor exists + is at prev checkpoint, we can updated
                expectedCheckpointInstance = storedCheckpointInstance;
            } else {                                                     // actor exists but is at newer instance, ignore update
                return false;
            }
            
            TransactionResult res = client.transaction(
                    new Transaction(true, queue -> {
                        queue.set(checkpointInstanceKey, checkpointInstance);
                        queue.set(dataKey, data);
                        queue.set(stateKey, STATE_IDLE);
                        if (checkpointPayload != null) {
                            queue.set(checkpointMsgKey, checkpointPayload);
                            queue.set(checkpointTimeKey, checkpointTime);
                        }
                    }),
                    // Before inserting, make sure the checkpoint instance in redis didn't change
                    new Watch(checkpointInstanceKey, false, () -> {
                        Integer existingCheckpointInstance = client.get(checkpointInstanceKey, InternalUtils::stringToInt);
                        return Objects.equals(expectedCheckpointInstance, existingCheckpointInstance);
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
            
            return res != null;
        } catch (ConnectionException ce) {
            throw new ClientException(ce.isConnectionProblem(), ce);
        } catch (RuntimeException re) {
            throw new ClientException(false, re);
        }
    }

    @Override
    public void putMessage(byte[] data) throws ClientException {
        Validate.notNull(data);

        try {
            client.transaction(
                    new Transaction(true, queue -> {
                        queue.lpush(msgQueueKey, data);
                    }),
                    new Watch(dataKey, false, () -> client.exists(dataKey)),
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
        } catch (ConnectionException ce) {
            throw new ClientException(ce.isConnectionProblem(), ce);
        } catch (RuntimeException re) {
            throw new ClientException(false, re);
        }
    }
    
    @Override
    public Work nextMessage() throws ClientException {
        try {
            TransactionResult ret = client.transaction(
                    // if the multi/exec block fails, do retry (retries the whole thing, including the watches)
                    new Transaction(true, queue -> {
                        queue.get(checkpointInstanceKey, InternalUtils::stringToInt);
                        queue.rpop(msgQueueKey);
                        queue.get(dataKey);
                        queue.set(stateKey, STATE_PROCESSING);
                    }),
                    // Make sure message queue is not empty and the actor isn't processing a message (is idle)
                    new Watch(msgQueueKey, false, () -> client.llen(msgQueueKey) > 0),
                    new Watch(stateKey, false, () -> {
                        String state = client.get(stateKey, InternalUtils::byteArrayToString);
                        return STATE_IDLE.equals(state);
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

            if (ret == null) {
                return null;
            }

            Validate.validState(ret.size() == 4); // sanity check
            
            return new Work(
                    (byte[]) ret.get(2),
                    (byte[]) ret.get(1),
                    ((Integer) ret.get(0)));
        } catch (ConnectionException ce) {
            throw new ClientException(ce.isConnectionProblem(), ce);
        } catch (RuntimeException re) {
            throw new ClientException(false, re);
        }
    }
    

    @Override
    public boolean isIdleAndHasMessages() throws ClientException {
        try {
            TransactionResult ret = client.transaction(
                    // if the multi/exec block fails, do retry (retries the whole thing, including the watches)
                    new Transaction(true, queue -> {
                        queue.get(stateKey, InternalUtils::byteArrayToString);
                        queue.exists(msgQueueKey); // msgQueueQueue won't exists if empty
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

            Validate.validState(ret.size() == 2); // sanity check
            
            return STATE_IDLE.equals(ret.get(0)) && (boolean) ret.get(1);
        } catch (ConnectionException ce) {
            throw new ClientException(ce.isConnectionProblem(), ce);
        } catch (RuntimeException re) {
            throw new ClientException(false, re);
        }
    }
    
    @Override
    public Work checkpointMessage(long currentTime) throws ClientException {
        Validate.isTrue(currentTime >= 0);
        try {
            TransactionResult ret = client.transaction(
                    // if the multi/exec block fails, do retry (retries the whole thing, including the watches)
                    new Transaction(true, queue -> {
                        queue.incr(checkpointInstanceKey);             // increment checkpoint instance
                        queue.set(checkpointTimeKey, Long.MAX_VALUE);  // max out checkpoint time to max so we don't hit checkpoint again
                        queue.get(checkpointMsgKey);
                        queue.get(dataKey);
                        queue.set(stateKey, STATE_PROCESSING);
                    }),
                    // Make sure checkpointTime exists and it's greater than currentTime.
                    new Watch(checkpointTimeKey, false, () -> {
                        Long checkpointTime = client.get(checkpointTimeKey, InternalUtils::stringToLong);
                        return checkpointTime != null && checkpointTime <= currentTime;
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

            if (ret == null) {
                return null;
            }

            Validate.validState(ret.size() == 5); // sanity check
            return new Work(
                    (byte[]) ret.get(3),
                    (byte[]) ret.get(2),
                    ((Long) ret.get(0)).intValue()
            );
        } catch (ConnectionException ce) {
            throw new ClientException(ce.isConnectionProblem(), ce);
        } catch (RuntimeException re) {
            throw new ClientException(false, re);
        }
    }
    
    @Override
    public void remove() throws ClientException {
        try {
            client.transaction(
                    new Transaction(true, queue -> {
                        queue.del(checkpointMsgKey);
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
        } catch (ConnectionException ce) {
            throw new ClientException(ce.isConnectionProblem(), ce);
        } catch (RuntimeException re) {
            throw new ClientException(false, re);
        }
    }







    private static final CharSequenceTranslator BRACE_REMOVAL_TRANSLATOR = new AggregateTranslator(
            new LookupTranslator(singletonMap("{", "\\u007B")),
            new LookupTranslator(singletonMap("}", "\\u007D")),
            new LookupTranslator(singletonMap("\\", "\\\\"))
    );

    private static String toClusterKey(String prefix, Address address, String suffix) {
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
}
