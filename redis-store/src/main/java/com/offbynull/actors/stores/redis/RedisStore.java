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

import com.offbynull.actors.stores.redis.client.TimestampQueue;
import com.offbynull.actors.stores.redis.client.Client;
import com.offbynull.actors.stores.redis.client.ActorAccessor;
import com.offbynull.actors.gateways.actor.SerializableActor;
import com.offbynull.actors.shuttle.Address;
import com.offbynull.actors.shuttle.Message;
import com.offbynull.actors.store.Store;
import com.offbynull.actors.store.StoredWork;
import com.offbynull.actors.stores.redis.client.ActorAccessor.Work;
import com.offbynull.actors.stores.redis.client.ClientException;
import com.offbynull.actors.stores.redis.client.ClientFactory;
import com.offbynull.actors.stores.redis.connectors.jedis.JedisPoolConnector;
import com.offbynull.actors.stores.memory.BestEffortSerializer;
import java.io.IOException;
import java.util.Collection;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.offbynull.actors.stores.redis.connector.Connector;
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
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisStore.class);
    
    private final String prefix;
    private final ClientFactory clientFactory;
    private final BestEffortSerializer serializer;
    private final Random random;
    
    private final QueueCount readTimestampQueueCount;
    private final QueueCount writeTimestampQueueCount;
    
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
     * Creates a {@link RedisStore} object.
     * @param prefix prefix for the actor gateway that this storage engine belongs to
     * @param host redis host
     * @param port redis port
     * @param cacheCount number of cached connections to the redis server
     * @param timestampQueueCount number of timestamp queues
     * @return new redis store
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code port} is invalid, or {@code cacheCount < 0}, or {@code timestampQueueCount <= 0}
     */
    public static RedisStore create(String prefix, String host, int port, int cacheCount, int timestampQueueCount) {
        Validate.notNull(prefix);
        Validate.notNull(host);
        Validate.isTrue(port > 0 && port <= 65535);
        Validate.isTrue(cacheCount >= 0);
        Validate.isTrue(timestampQueueCount > 0);
        
        Connector connector = new JedisPoolConnector(host, port, cacheCount);
        ClientFactory clientFactory = new ConnectorClientFactory(connector);
        return new RedisStore(
                prefix,
                clientFactory,
                new QueueCount(timestampQueueCount),
                new QueueCount(timestampQueueCount));
    }

    /**
     * Creates a {@link RedisStore} object.
     * <p>
     * This create method allows you to supply {@link QueueCount} objects to control how many queues are used. In times of increase load,
     * you can relieve pressure on the queues by increasing the number of read queues and write queues. Once load decreases, you can
     * rollback the number of write queues while keeping the number of read queues the same until they clear out, then rollback the number
     * of read queues as well.
     * <p>
     * Remember that {@link QueueCount} objects are thread safe, you can manipulate them as you need to.
     * @param prefix prefix for the actor gateway that this storage engine belongs to
     * @param clientFactory client factory to use for interfacing with redis
     * @param readTimestampQueueCount number of timestamp queues to read from (will access randomly queues from 0 to
     * {@code readTimestampQueueCount-1}}
     * @param writeTimestampQueueCount number of timestamp queues to write to (will access randomly queues from 0 to
     * {@code writeTimestampQueueCount-1}}
     * @return new redis store
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code port} is invalid, or {@code cacheCount < 0}, or {@code timestampQueueCount <= 0}
     */
    public static RedisStore create(
            String prefix,
            ClientFactory clientFactory,
            QueueCount readTimestampQueueCount,
            QueueCount writeTimestampQueueCount) {
        Validate.notNull(prefix);
        Validate.notNull(clientFactory);
        Validate.notNull(readTimestampQueueCount);
        Validate.notNull(writeTimestampQueueCount);
        return new RedisStore(prefix, clientFactory, readTimestampQueueCount, writeTimestampQueueCount);
    }
    
    private RedisStore(
            String prefix,
            ClientFactory clientFactory,
            QueueCount readTimestampQueueCount,
            QueueCount writeTimestampQueueCount) {
        Validate.notNull(prefix);
        Validate.notNull(clientFactory);
        Validate.notNull(readTimestampQueueCount);
        Validate.notNull(writeTimestampQueueCount);
        
        this.prefix = prefix;
        this.clientFactory = clientFactory;
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
        Validate.validState(actorAddr.size() == 2, "Actor address has unexpected number of elements: %s", actorAddr);
        Validate.validState(actorAddr.getElement(0).equals(prefix), "Actor address must start with %s: %s", prefix, actorAddr);
        
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
            try (Client client = clientFactory.getClient()) {
                Instant currentInstant = Instant.now();
                long currentTime = currentInstant.toEpochMilli();
                long checkpointTime = -1L;
                if (checkpointUpdated) {
                    long checkpointTimeout = actor.getCheckpointTimeout();
                    checkpointTime = calculateCheckpointTime(currentInstant, checkpointTimeout);
                }
                
                ActorAccessor actorAccessor = client.getActorAccessor(actorAddr);
                boolean written = actorAccessor.update(actorData, checkpointPayloadData, checkpointTime, checkpointInstance);

                randomWriteMessageQueue(client).insert(currentTime, actorAddr);
                if (written && checkpointUpdated) {
                    randomWriteCheckpointQueue(client).insert(checkpointTime, actorAddr);
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
            Validate.validState(dstAddr.size() >= 2, "Actor address must have atleast 2 elements: %s", dstAddr);
            Validate.validState(dstAddr.getElement(0).equals(prefix), "Actor address must start with %s: %s", prefix, dstAddr);
        });

        for (Message message : messages) {
            Address dstAddr = Address.of(prefix, message.getDestinationAddress().getElement(1));
            byte[] messageData = serializer.serialize(message);

            retry(() -> {
                try (Client client = clientFactory.getClient()) {
                    ActorAccessor actorAccessor = client.getActorAccessor(dstAddr);
                    actorAccessor.putMessage(messageData);
                    
                    long currentTime = Instant.now().toEpochMilli();
                    if (actorAccessor.isIdleAndHasMessages()) {
                        // we didn't pull any messages, but the actor is idle so we can re-queue and try again
                        randomWriteMessageQueue(client).insert(currentTime, dstAddr);
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
            try (Client client = clientFactory.getClient()) {
                ActorAccessor actorAccessor = client.getActorAccessor(address);
                actorAccessor.remove();
            }
        });
    }

    @Override
    public StoredWork take() {
        Validate.validState(!closed, "Store closed");
        
        Work work = retry(() -> {
            while (true) {
                try (Client client = clientFactory.getClient()) {
                    long currentTime = Instant.now().toEpochMilli();
                    Address address;

                    
                    
                    // CHECK FOR NEW MESSAGE AND RETURN IF FOUND
                    address = randomReadMessageQueue(client).remove(currentTime);
                    if (address != null) {
                        ActorAccessor actorAccessor = client.getActorAccessor(address);
                        Work messageWork = actorAccessor.nextMessage();

                        // if we pulled work, return it...
                        if (messageWork != null) {
                            return messageWork;
                        }

                        // if we didn't pull work but the actor is idle, re-queue and move on to checking for a checkpoint...
                        if (actorAccessor.isIdleAndHasMessages()) {
                            randomWriteMessageQueue(client).insert(currentTime, address);
                        }
                    }


                    
                    // CHECK IF CHECKPOINT HIT AND RETURN IF FOUND
                    address = randomReadCheckpointQueue(client).remove(currentTime);
                    if (address != null) {
                        ActorAccessor actorAccessor = client.getActorAccessor(address);
                        Work checkpointWork = actorAccessor.checkpointMessage(currentTime);
                        
                        // if a checkpoint was hit, return it...
                        if (checkpointWork != null) {
                            return checkpointWork;
                        }

                        // we didn't pull a checkpoint for whatever reason? maybe the actor was removed... retry the entire block by letting
                        // it loop again
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
        clientFactory.close();
    }
    
    
    
    
    
    
    
    
    private static long calculateCheckpointTime(Instant currentInstant, long timeout) {
        try {
            return currentInstant.plusMillis(timeout).toEpochMilli();
        } catch (ArithmeticException ae) {
            return Long.MAX_VALUE;
        }
    }
    
    
    
    
    
    
    
    
    private TimestampQueue randomReadMessageQueue(Client client) {
        int maxIdx = readTimestampQueueCount.getCount();
        int queueIdx = random.nextInt(maxIdx);
        return client.getMessageCheckQueue(queueIdx);
    }

    private TimestampQueue randomWriteMessageQueue(Client client) {
        int maxIdx = writeTimestampQueueCount.getCount();
        int queueIdx = random.nextInt(maxIdx);
        return client.getMessageCheckQueue(queueIdx);
    }
    
    private TimestampQueue randomReadCheckpointQueue(Client client) {
        int maxIdx = readTimestampQueueCount.getCount();
        int queueIdx = random.nextInt(maxIdx);
        return client.getCheckpointCheckQueue(queueIdx);
    }

    private TimestampQueue randomWriteCheckpointQueue(Client client) {
        int maxIdx = writeTimestampQueueCount.getCount();
        int queueIdx = random.nextInt(maxIdx);
        return client.getCheckpointCheckQueue(queueIdx);
    }
    
    
    
    
    
    
    private <V> V retry(RetryReturnWrapper<V> wrapper) {
        while (true) {
            Validate.validState(!closed, "Store closed");
            try {
                return wrapper.run();
            } catch (ClientException e) {
                if (e.isConnectionProblem()) {
                    LOGGER.error("Connection problem encountered, retrying...", e);
                } else {
                    LOGGER.error("Non-connection problem encountered", e);
                    throw new IllegalStateException(e);
                }
            } catch (IOException e) {
                LOGGER.error("IO problem encountered, retrying...", e);
            }
        }
    }
    
    private interface RetryReturnWrapper<V> {
        V run() throws IOException, ClientException;;
    }

    private void retry(RetryWrapper wrapper) {
        while (true) {
            Validate.validState(!closed, "Store closed");
            try {
                wrapper.run();
                return;
            } catch (ClientException e) {
                if (e.isConnectionProblem()) {
                    LOGGER.error("Connection problem encountered, retrying...", e);
                } else {
                    LOGGER.error("Non-connection problem encountered", e);
                    throw new IllegalStateException(e);
                }
            } catch (IOException e) {
                LOGGER.error("IO problem encountered, retrying...", e);
            }
        }
    }
    
    private interface RetryWrapper {
        void run() throws IOException, ClientException;
    }
    
    
    
    
    
    

    /**
     * Controls the number of timestamp queues. This count can be modified even after being passed into {@link RedisStore}.
     * <p>
     * Increase to spread load over multiple queues in times of high-load, then decrease to reduce the number of queues back down.
     */
    public static final class QueueCount {
        private volatile int count;

        /**
         * Constructs a {@link QueueCount} object.
         * @param count initial number of queues
         * @throws IllegalArgumentException if {@code count <= 0}
         */
        public QueueCount(int count) {
            Validate.isTrue(count >= 1);
            this.count = count;
        }

        /**
         * Get queue count.
         * @return queue count
         */
        public int getCount() {
            return count;
        }

        /**
         * Set queue count.
         * @param count queue count
         * @throws IllegalArgumentException if {@code count <= 0}
         */
        public void setCount(int count) {
            Validate.isTrue(count >= 1);
            this.count = count;
        }
    }
}
