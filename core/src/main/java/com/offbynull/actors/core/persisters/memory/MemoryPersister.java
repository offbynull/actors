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
package com.offbynull.actors.core.persisters.memory;

import com.offbynull.actors.core.gateways.actor.SerializableActor;
import com.offbynull.actors.core.persister.PersisterWork;
import com.offbynull.actors.core.shuttle.Address;
import com.offbynull.actors.core.persister.Persister;
import com.offbynull.actors.core.shuttle.Message;
import java.nio.ByteBuffer;
import static java.nio.ByteBuffer.wrap;
import java.time.Instant;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.TreeSet;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

/**
 * A persister that keeps all actors and messages serialized in memory.
 * @author Kasra Faghihi
 */
public final class MemoryPersister implements Persister {
    private final UnmodifiableList<LockRegion> lockRegions;
    private final String prefix;
    private volatile boolean closed;

    /**
     * Constructs a {@link MemoryPersister} object.
     * @param prefix prefix for the actor gateway that this persister belongs to
     * @param concurrency concurrency level (should be set to number of cores or larger)
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code concurrency <= 0}
     */
    public MemoryPersister(String prefix, int concurrency) {
        Validate.notNull(prefix);
        Validate.isTrue(concurrency > 0);

        LockRegion[] regions = new LockRegion[concurrency];
        for (int i = 0; i < regions.length; i++) {
            regions[i] = new LockRegion();
        }

        this.prefix = prefix;
        this.lockRegions = (UnmodifiableList<LockRegion>) unmodifiableList(new ArrayList<>(asList(regions)));
        this.closed = false;
    }

    @Override
    public void store(SerializableActor actor) {
        Validate.notNull(actor);
        Validate.validState(!closed, "Persister closed");
        
        Address actorAddr = actor.getSelf();
        Validate.validState(actorAddr.size() == 2, "Actor address has unexpected number of elements: %s", actorAddr);
        Validate.validState(actorAddr.getElement(0).equals(prefix), "Actor address must start with %s: %s", prefix, actorAddr);

        LockRegion lockRegion = getLockRegion(actorAddr);
        synchronized (lockRegion) {
            // Create or replace actordata
            ActorData actorData = lockRegion.actors.computeIfAbsent(actorAddr, x -> {
                Validate.validState(actor.getCheckpointTimeout() > 0L && actor.getCheckpointMessage() != null,
                        "Initial store of an actor needs a checkpoint time and message: %s", actorAddr);

                ActorData newActorData = new ActorData();
                newActorData.msgQueue = new LinkedList<>();

                return newActorData;
            });
            
            // Serialize actor
            byte[] serializedActor = lockRegion.serializer.serialize(actor);
            actorData.data = serializedActor;

            // If supplied, update the checkpointing timeout and set the current data being added as a checkpoint
            long checkpointTimeout = actor.getCheckpointTimeout();
            if (checkpointTimeout > 0L) {
                Validate.validState(actor.getCheckpointMessage() != null, "Checkpoint has timeout but no message: %s", actorAddr);

                actorData.checkpointTimeout = checkpointTimeout;
                actorData.checkpointData = serializedActor;
            }

            // Update checkpoint timeout value
              // must remove and re-add -- collection is using staletime to sort
            lockRegion.timeouts.remove(actorData);
            actorData.checkpointStaleTime = Instant.now().plusMillis(actorData.checkpointTimeout);
            lockRegion.timeouts.add(actorData);

            // If msgs are available, add to availableSet. If is being put back into storage after processing, remove from processingSet
            if (!actorData.msgQueue.isEmpty()) {
                lockRegion.availableSet.add(actorAddr);
            }
            lockRegion.processingSet.remove(actorAddr);
        }
    }

    @Override
    public void store(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        Validate.validState(!closed, "Persister closed");

        for (Message message : messages) {
            Address dstAddr = message.getDestinationAddress();
            Validate.validState(dstAddr.size() >= 2, "Actor address must have atleast 2 elements: %s", dstAddr);
            Validate.validState(dstAddr.getElement(0).equals(prefix), "Actor address must start with %s: %s", prefix, dstAddr);
            Address dstActorAddr = Address.of(prefix, dstAddr.getElement(1));

            LockRegion lockRegion = getLockRegion(dstActorAddr);
            synchronized (lockRegion) {
                ActorData dstActorData = lockRegion.actors.get(dstActorAddr);

                if (dstActorData != null) {
                    byte[] serializedMsg = lockRegion.serializer.serialize(message);
                    dstActorData.msgQueue.addLast(serializedMsg);
                    lockRegion.pendingMsgCount++;
                    
                    // mark as available if not processing
                    if (!lockRegion.processingSet.contains(dstActorAddr)) {
                        lockRegion.availableSet.add(dstActorAddr);
                    }
                }
            }
        }
    }
    
    @Override
    public void discard(Address address) {
        Validate.notNull(address);
        Validate.validState(!closed, "Persister closed");

        Address lockAddr = address;
        LockRegion lockRegion = getLockRegion(lockAddr);
        synchronized (lockRegion) {
            Address actorAddr = address;
            ActorData actorData = lockRegion.actors.remove(actorAddr);
            if (actorData != null) {
                lockRegion.timeouts.remove(actorData);
                lockRegion.availableSet.remove(actorAddr);
                lockRegion.processingSet.remove(actorAddr);
            }
        }       
    }

    @Override
    public PersisterWork take() {
        while (true) {
            Validate.validState(!closed, "Persister closed");

            LockRegion lockRegion = randomizeLockRegion();
            synchronized (lockRegion) {
                if (!lockRegion.availableSet.isEmpty()) { // something waiting? if so, grab it and return it
                    // Get next available
                    Address actorAddr = lockRegion.availableSet.iterator().next();

                    // Remove message and deserialize it + deserialize the actor
                    ActorData actorData = lockRegion.actors.get(actorAddr);
                    byte[] serializedMsg = actorData.msgQueue.removeFirst();
                    byte[] serializedActor = actorData.data;
                    Message msg = lockRegion.serializer.deserialize(serializedMsg);
                    SerializableActor actor = lockRegion.serializer.deserialize(serializedActor);

                    lockRegion.pendingMsgCount--;

                    // Remove from available and add to processing, also remove from timeouts because we don't want the actor triggering the
                    // stale message while it's processing
                    lockRegion.availableSet.remove(actorAddr);
                    lockRegion.timeouts.remove(actorData);
                    lockRegion.processingSet.add(actorAddr);
                    
                    return new PersisterWork(msg, actor);
                } else if (!lockRegion.timeouts.isEmpty()) { // otherwise, any stale actors? timeouts only contain non-processing actors
                    ActorData actorData = lockRegion.timeouts.first();
                    
                    Instant now = Instant.now();
                    Instant checkpointTime = actorData.checkpointStaleTime;

                    if (now.isAfter(checkpointTime) || now.equals(checkpointTime)) {
                        byte[] serializedActor = actorData.checkpointData;
                        SerializableActor actor = lockRegion.serializer.deserialize(serializedActor);
                        
                        Address actorAddr = actor.getSelf();
                        Object checkpointMsg = actor.getCheckpointMessage();
                        Message msg = new Message(actorAddr, actorAddr, checkpointMsg);

                        lockRegion.availableSet.remove(actorAddr);
                        lockRegion.timeouts.remove(actorData);
                        lockRegion.processingSet.add(actorAddr);

                        return new PersisterWork(msg, actor);
                    }
                }
            }

            // Since this method is being called in a tight loop by the actor system we need to add in a Thread.sleep() here or something to
            // stop the CPU usage from spiking out of control... note that we're out of the synchronized block so we aren't causing an
            // unnessecary delay with the lock (maybe someone was trying to put stuff into this lock region)
            try {
                Thread.sleep(0);
            } catch (InterruptedException ie) {
                throw new IllegalStateException(ie);
            }
        }
    }

    @Override
    public void close() {
        closed = true;
    }

    /**
     * Get the number of messages that require processing.
     * @return number of actors currently processing
     * @throws IllegalStateException if persister has been closed
     */
    public int getStoredMessageCount() {
        Validate.validState(!closed, "Persister closed");
        
        int ret = 0;
        for (LockRegion lockRegion : lockRegions) {
            synchronized (lockRegion) {
                ret += lockRegion.pendingMsgCount;
            }
        }
        
        return ret;
    }

    /**
     * Get the number of actors that are stored -- including those idle, awaiting processing, being processed, and stale (approximate).
     * @return number of actors currently processing
     * @throws IllegalStateException if persister has been closed
     */
    public int getActorCount() {
        Validate.validState(!closed, "Persister closed");
        
        int ret = 0;
        for (LockRegion lockRegion : lockRegions) {
            synchronized (lockRegion) {
                ret += lockRegion.actors.size();
            }
        }
        
        return ret;
    }

    /**
     * Get the number of actors that are currently processing a message (approximate).
     * @return number of actors currently processing
     * @throws IllegalStateException if persister has been closed
     */
    public int getProcessingActorCount() {
        Validate.validState(!closed, "Persister closed");
        
        int ret = 0;
        for (LockRegion lockRegion : lockRegions) {
            synchronized (lockRegion) {
                ret += lockRegion.processingSet.size();
            }
        }
        
        return ret;
    }

    /**
     * Get the number of actors that have pending messages and are awaiting processing (approximate).
     * @return number of actors awaiting processing
     * @throws IllegalStateException if persister has been closed
     */
    public int getReadyActorCount() {
        Validate.validState(!closed, "Persister closed");
        
        int ret = 0;
        for (LockRegion lockRegion : lockRegions) {
            synchronized (lockRegion) {
                ret += lockRegion.availableSet.size();
            }
        }
        
        return ret;
    }












    
    private LockRegion getLockRegion(Address key) {
        String keyStr = key.toString();
        byte[] hash = DigestUtils.md5(keyStr);
        int idx = Math.abs(wrap(hash).getInt() % lockRegions.size());
        
        return lockRegions.get(idx);
    }
    
    private LockRegion randomizeLockRegion() {
        long nanoTime = System.nanoTime();
        int threadHashCode = Thread.currentThread().hashCode();
        ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES + Integer.BYTES);
        byteBuffer.putLong(nanoTime);
        byteBuffer.putInt(threadHashCode);
        
        byte[] dataToHash = byteBuffer.array();
        byte[] hash = DigestUtils.md5(dataToHash);
        
        int idx = Math.abs(wrap(hash).getInt() % lockRegions.size());
        
        return lockRegions.get(idx);
    }




    private static final class LockRegion {
        private final BestEffortSerializer serializer = new BestEffortSerializer();
        private final HashMap<Address, ActorData> actors = new HashMap<>();         // actor addr -> persisted actor obj
        private final TreeSet<ActorData> timeouts = new TreeSet<>((x, y) -> {
            int ret = x.checkpointStaleTime.compareTo(y.checkpointStaleTime);
            if (ret == 0 && x != y) { // if we ever encounter the same time (but different objs), treat it as less-than -- we do this
                                      // because we're using this set just to order (we still want duplicates showing up)
                ret = -1;
            }
            return ret;
        }); // timeout -> actor addr
        
        private int pendingMsgCount; // cache of messages waiting to be processed
        
        private final LinkedHashSet<Address> availableSet = new LinkedHashSet<>();  // actors that aren't processing but have msgs ready
        private final LinkedHashSet<Address> processingSet = new LinkedHashSet<>(); // actors currently processing a msg
    }
    
    private static final class ActorData {
        private byte[] data;
        private byte[] checkpointData;
        private long checkpointTimeout;
        private Instant checkpointStaleTime;
        private LinkedList<byte[]> msgQueue;
    }
}
