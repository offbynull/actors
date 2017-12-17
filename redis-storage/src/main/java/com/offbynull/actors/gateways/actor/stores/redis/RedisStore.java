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

import com.offbynull.actors.gateways.actor.SerializableActor;
import com.offbynull.actors.address.Address;
import com.offbynull.actors.shuttle.Message;
import com.offbynull.actors.gateways.actor.Store;
import com.offbynull.actors.redisclients.jedis.JedisPoolConnector;
import com.offbynull.actors.common.BestEffortSerializer;
import com.offbynull.actors.gateways.actor.stores.redis.ActorAccessor.Work;
import com.offbynull.actors.redisclient.Connection;
import java.io.IOException;
import java.util.Collection;
import org.apache.commons.lang3.Validate;
import com.offbynull.actors.redisclient.Connector;
import static com.offbynull.actors.redisclient.RedisUtils.retry;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Random;

/**
 * A storage engine that keeps all actors and messages serialized in Redis.
 * <p>
 * This storage engine makes use of Redis sorted sets as queues to keep track of which actors need processing (message available or
 * checkpoint hit). These queues are accessed very frequently using WATCH/MULTI/EXEC transactions, meaning that in times of high-load you
 * may end up having transactions clobber over each other and fail. While these failures are handled automatically (the system will retry
 * until its successful), the throughput of the queues may be effected.
 * <p>
 * To work around this issue, you have the option of dynamically expanding and contracting the number of queues you read/write from. In
 * times of increase load, you can relieve pressure on the queues by increasing the number of read queues and write queues. Once load
 * decreases, you can rollback the number of write queues while keeping the number of read queues the same until they clear out, then
 * rollback the number of read queues as well.
 * <p>
 * This storage engine has been tested with a non-clustered Redis instance, but has been designed in such a way that it can be extended
 * to work with a clustered Redis instance: Redis hash tags are used to keep keys that are accessed together on the same Redis node, such
 * that they can all be queried/changed consistently in a WATCH/MULTI/EXEC transaction. These WATCH/MULTI/EXEC transactions also make sure
 * that a failure doesn't happen from data migrating between nodes.
 * @author Kasra Faghihi
 */
public final class RedisStore implements Store {
    
    // This storage engine has been designed to work with a single Redis server, but can easily be extended to work with a Redis cluster.
    
    private final String prefix;
    private final Connector connector;
    private final BestEffortSerializer serializer;
    private final Random random;
    
    private final QueueCountController readTimestampQueueCount;
    private final QueueCountController writeTimestampQueueCount;
    
    private volatile boolean closed;
    
    /**
     * Creates a {@link RedisStore} object. Equivalent to calling {@code create(prefix, host, port, 10, 5) }.
     * @param prefix prefix for the actor gateway that this storage engine belongs to
     * @param host redis host
     * @param port redis port
     * @return new redis store
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code port} is invalid
     */
    public static RedisStore create(String prefix, String host, int port) {
        return create(prefix, host, port, 10, 5);
    }
    
    /**
     * Creates a {@link RedisStore} object. Equivalent to calling {@code create(prefix, new JedisPoolConnector(host, port, cacheCount), 
     * new QueueCountController(queueCount), new QueueCountController(queueCount)) }.
     * @param prefix prefix for the actor gateway that this storage engine belongs to
     * @param host redis host
     * @param port redis port
     * @param cacheCount number of cached connections to the redis server
     * @param queueCount number of timestamp queues
     * @return new redis store
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code port} is invalid, or {@code cacheCount < 0}, or {@code timestampQueueCount <= 0}
     */
    public static RedisStore create(String prefix, String host, int port, int cacheCount, int queueCount) {
        Validate.notNull(prefix);
        Validate.notNull(host);
        Validate.isTrue(port > 0 && port <= 65535);
        Validate.isTrue(cacheCount >= 0);
        Validate.isTrue(queueCount > 0);
        
        return create(
                prefix,
                new JedisPoolConnector(host, port, cacheCount),
                new QueueCountController(queueCount),
                new QueueCountController(queueCount));
    }

    /**
     * Creates a {@link RedisStore} object.
     * <p>
     * This create method allows you to supply {@link QueueCountController} objects to control how many queues are used. In times of
     * increase load, you can relieve pressure on the queues by increasing the number of read queues and write queues. Once load decreases,
     * you can rollback the number of write queues while keeping the number of read queues the same until they clear out, then rollback the
     * number of read queues as well.
     * <p>
     * Remember that {@link QueueCountController} objects are thread safe, you can manipulate them as you need to.
     * @param prefix prefix for the actor gateway that this storage engine belongs to
     * @param connector connector to use for creating redis connections
     * @param readTimestampQueueCount number of timestamp queues to read from (will access randomly queues from 0 to
     * {@code readTimestampQueueCount-1}}
     * @param writeTimestampQueueCount number of timestamp queues to write to (will access randomly queues from 0 to
     * {@code writeTimestampQueueCount-1}}
     * @return new redis store
     * @throws NullPointerException if any argument is {@code null}
     */
    public static RedisStore create(
            String prefix,
            Connector connector,
            QueueCountController readTimestampQueueCount,
            QueueCountController writeTimestampQueueCount) {
        Validate.notNull(prefix);
        Validate.notNull(connector);
        Validate.notNull(readTimestampQueueCount);
        Validate.notNull(writeTimestampQueueCount);
        return new RedisStore(prefix, connector, readTimestampQueueCount, writeTimestampQueueCount);
    }
    
    private RedisStore(
            String prefix,
            Connector connector,
            QueueCountController readTimestampQueueCount,
            QueueCountController writeTimestampQueueCount) {
        Validate.notNull(prefix);
        Validate.notNull(connector);
        Validate.notNull(readTimestampQueueCount);
        Validate.notNull(writeTimestampQueueCount);
        
        this.prefix = prefix;
        this.connector = connector;
        this.serializer = new BestEffortSerializer();
        this.readTimestampQueueCount = readTimestampQueueCount;
        this.writeTimestampQueueCount = writeTimestampQueueCount;
        try {
            this.random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException(nsae); // should never happen?
        }
    }

    @Override
    public void store(SerializableActor actor) {
        Validate.notNull(actor);
        Validate.validState(!closed, "Store closed");

        Address actorAddr = actor.getSelf();
        Validate.isTrue(actorAddr.size() == 2, "Actor address has unexpected number of elements: %s", actorAddr);
        Validate.isTrue(actorAddr.getElement(0).equals(prefix), "Actor address must start with %s: %s", prefix, actorAddr);
        
        byte[] actorData = serializer.serialize(actor);
        
        int checkpointInstance = actor.getCheckpointInstance();
        boolean checkpointUpdated = actor.getCheckpointUpdated();
        byte[] checkpointPayloadData;
        if (checkpointUpdated) {
            Object checkpointPayload = actor.getCheckpointPayload();
            checkpointPayloadData = serializer.serialize(new Message(actorAddr, actorAddr, checkpointPayload));
        } else {
            checkpointPayloadData = null;
        }

        retry(() -> {
            Validate.validState(!closed, "Store closed");
            
            try (Connection connection = connector.getConnection()) {
                Instant currentInstant = Instant.now();
                long currentTime = currentInstant.toEpochMilli();
                long checkpointTime = -1L;
                if (checkpointUpdated) {
                    long checkpointTimeout = actor.getCheckpointTimeout();
                    checkpointTime = calculateCheckpointTime(currentInstant, checkpointTimeout);
                }
                
                ActorAccessor actorAccessor = new ActorAccessor(connection, actorAddr);
                boolean written = actorAccessor.update(actorData, checkpointPayloadData, checkpointTime, checkpointInstance);

                randomWriteMessageQueue(connection).insert(currentTime, actorAddr);
                if (written && checkpointUpdated) {
                    randomWriteCheckpointQueue(connection).insert(checkpointTime, actorAddr);
                }
            }
        });
    }

    @Override
    public void store(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        Validate.validState(!closed, "Store closed");
        messages.forEach(m -> {
            Address dstAddr = m.getDestinationAddress();
            Validate.isTrue(dstAddr.size() >= 2, "Actor address must have atleast 2 elements: %s", dstAddr);
            Validate.isTrue(dstAddr.getElement(0).equals(prefix), "Actor address must start with %s: %s", prefix, dstAddr);
        });

        for (Message message : messages) {
            Address dstAddr = Address.of(prefix, message.getDestinationAddress().getElement(1));
            byte[] messageData = serializer.serialize(message);

            retry(() -> {
                Validate.validState(!closed, "Store closed");
                
                try (Connection connection = connector.getConnection()) {
                    ActorAccessor actorAccessor = new ActorAccessor(connection, dstAddr);
                    actorAccessor.putMessage(messageData);
                    
                    long currentTime = Instant.now().toEpochMilli();
                    if (actorAccessor.isIdleAndHasMessages()) {
                        // we didn't pull any messages, but the actor is idle so we can re-queue and try again
                        randomWriteMessageQueue(connection).insert(currentTime, dstAddr);
                    }
                }
            });
        }
    }

    @Override
    public void discard(Address address) {
        Validate.isTrue(address.size() == 2);
        Validate.isTrue(address.getElement(0).equals(prefix));
        Validate.validState(!closed, "Store closed");

        retry(() -> {
            Validate.validState(!closed, "Store closed");
            
            try (Connection connection = connector.getConnection()) {
                ActorAccessor actorAccessor = new ActorAccessor(connection, address);
                actorAccessor.remove();
            }
        });
    }

    @Override
    public StoredWork take() {
        Validate.validState(!closed, "Store closed");
        
        Work work = retry(() -> {
            while (true) {
                Validate.validState(!closed, "Store closed");

                try (Connection connection = connector.getConnection()) {
                    long currentTime = Instant.now().toEpochMilli();
                    Address address;


                    
                    // CHECK IF CHECKPOINT HIT AND RETURN IF FOUND
                    address = randomReadCheckpointQueue(connection).remove(currentTime);
                    if (address != null) {
                        ActorAccessor actorAccessor = new ActorAccessor(connection, address);
                        Work checkpointWork = actorAccessor.checkpointMessage(currentTime);
                        
                        // if a checkpoint was hit, return it...
                        if (checkpointWork != null) {
                            return checkpointWork;
                        }

                        // we didn't pull a checkpoint for whatever reason? maybe the actor was removed... retry the entire block by letting
                        // it loop again
                    }
                    
                    
                    
                    
                    
                    // CHECK FOR NEW MESSAGE AND RETURN IF FOUND
                    address = randomReadMessageQueue(connection).remove(currentTime);
                    if (address != null) {
                        ActorAccessor actorAccessor = new ActorAccessor(connection, address);
                        Work messageWork = actorAccessor.nextMessage();

                        // if we pulled work, return it...
                        if (messageWork != null) {
                            return messageWork;
                        }

                        // if we didn't pull work but the actor is idle, re-queue and move on to checking for a checkpoint...
                        if (actorAccessor.isIdleAndHasMessages()) {
                            randomWriteMessageQueue(connection).insert(currentTime, address);
                        }
                    }
                }
            }
        });

        byte[] actorData = work.getActorData();
        byte[] messageData = work.getMessageData();

        SerializableActor actor = serializer.deserialize(actorData);
        Message msg = serializer.deserialize(messageData);
        
        actor.setCheckpointInstance(work.getCheckpointInstance());
        actor.setCheckpointUpdated(true);

        return new StoredWork(msg, actor);
    }

    @Override
    public void close() throws IOException {
        closed = true;
        connector.close();
    }
    
    
    
    
    
    
    
    
    private static long calculateCheckpointTime(Instant currentInstant, long timeout) {
        try {
            return currentInstant.plusMillis(timeout).toEpochMilli();
        } catch (ArithmeticException ae) {
            return Long.MAX_VALUE;
        }
    }
    
    
    
    
    
    
    
    private static final String MESSAGE_QUEUE_NAME = "message";
    
    private TimestampQueue randomReadMessageQueue(Connection connection) {
        int maxIdx = readTimestampQueueCount.getCount();
        int queueIdx = random.nextInt(maxIdx);
        return new TimestampQueue(connection, MESSAGE_QUEUE_NAME, queueIdx);
    }

    private TimestampQueue randomWriteMessageQueue(Connection connection) {
        int maxIdx = writeTimestampQueueCount.getCount();
        int queueIdx = random.nextInt(maxIdx);
        return new TimestampQueue(connection, MESSAGE_QUEUE_NAME, queueIdx);
    }
    
    
    
    private static final String CHECKPOINT_QUEUE_NAME = "checkpoint";
    
    private TimestampQueue randomReadCheckpointQueue(Connection connection) {
        int maxIdx = readTimestampQueueCount.getCount();
        int queueIdx = random.nextInt(maxIdx);
        return new TimestampQueue(connection, CHECKPOINT_QUEUE_NAME, queueIdx);
    }

    private TimestampQueue randomWriteCheckpointQueue(Connection connection) {
        int maxIdx = writeTimestampQueueCount.getCount();
        int queueIdx = random.nextInt(maxIdx);
        return new TimestampQueue(connection, CHECKPOINT_QUEUE_NAME, queueIdx);
    }
}
