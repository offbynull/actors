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
package com.offbynull.actors.gateways.servlet.stores.memory;

import com.offbynull.actors.common.BestEffortSerializer;
import com.offbynull.actors.gateways.servlet.Store;
import com.offbynull.actors.shuttle.Address;
import com.offbynull.actors.shuttle.Message;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import org.apache.commons.lang3.Validate;
import java.io.IOException;
import java.time.Clock;
import static java.util.stream.Collectors.toList;

/**
 * A storage engine that keeps all messages serialized in memory. Messages are evicted after a user-defined timeout.
 * @author Kasra Faghihi
 */
public final class MemoryStore implements Store {
    
    private final String prefix;
    
    private final LockRegion[] regions;
    private final Duration timeoutDuration;
    private final Clock clock;
    
    private volatile boolean closed;
    
    /**
     * Creates a {@link MemoryStore} object.
     * @param prefix prefix for the actor gateway that this storage engine belongs to
     * @param concurrency concurrency level (should be set to number of cores or larger)
     * @param timeout timeout duration -- if messages are read by the time this timeout hits, they are discarded
     * @return new memory store
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code concurrency <= 0}
     */
    public static MemoryStore create(String prefix, int concurrency, Duration timeout) {
        Validate.notNull(prefix);
        Validate.isTrue(concurrency > 0);
        Validate.isTrue(!timeout.isNegative() && !timeout.isZero());
        return new MemoryStore(prefix, concurrency, timeout);
    }
    
    MemoryStore(String prefix, int concurrency, Duration timeout) {
        this(prefix, concurrency, timeout, Clock.systemDefaultZone());
    }
    
    MemoryStore(String prefix, int concurrency, Duration timeout, Clock clock) {
        Validate.notNull(prefix);
        Validate.notNull(timeout);
        Validate.notNull(clock);
        Validate.isTrue(concurrency > 0);
        Validate.isTrue(!timeout.isNegative() && !timeout.isZero());
        
        this.prefix = prefix;
        
        regions = new LockRegion[concurrency];
        for (int i = 0; i < regions.length; i++) {
            regions[i] = new LockRegion();
        }
        
        timeoutDuration = timeout;
        this.clock = clock;
    }

    @Override
    public void write(String id, List<Message> messages) {
        Validate.notNull(id);
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        Validate.validState(!closed, "Store closed");
        messages.forEach(m -> {
            Address dstAddr = m.getDestinationAddress();
            Validate.isTrue(dstAddr.size() >= 2, "Actor address must have atleast 2 elements: %s", dstAddr);
            Validate.isTrue(dstAddr.getElement(0).equals(prefix), "Actor address must start with %s: %s", prefix, dstAddr);
        });
        
        LockRegion lockRegion = getLockRegion(id);
        synchronized (lockRegion) {
            filterTimedOutContainers(lockRegion);
            
            Queue queue = lockRegion.queues.computeIfAbsent(id, k -> new Queue(id));
            
            lockRegion.timeouts.remove(queue);
            queue.lastAccessTime = clock.instant();
            lockRegion.timeouts.add(queue);

            List<byte[]> serializedMesssages = messages.stream()
                    .map(m -> lockRegion.serializer.serialize(m))
                    .collect(toList());
            queue.data.addAll(serializedMesssages);
        }
    }

    @Override
    public List<Message> read(String id) {
        Validate.notNull(id);
        Validate.validState(!closed, "Store closed");
        
        LockRegion lockRegion = getLockRegion(id);
        synchronized (lockRegion) {
            filterTimedOutContainers(lockRegion);
            
            Queue queue = lockRegion.queues.get(id);
            if (queue == null) {
                return new ArrayList<>();
            }
            
            List<byte[]> serializedMessages = new ArrayList<>(queue.data);
            List<Message> messsages = serializedMessages.stream()
                    .map(b -> (Message) lockRegion.serializer.deserialize(b))
                    .collect(toList());

            lockRegion.queues.remove(queue.id);
            lockRegion.timeouts.remove(queue);
            
            return messsages;
        }
    }
    


    private void filterTimedOutContainers(LockRegion region) {
        Instant now = clock.instant();

        Iterator<Queue> i = region.timeouts.iterator();
        while (i.hasNext()) {
            Queue queue = i.next();
            Duration idleDuration = Duration.between(queue.lastAccessTime, now);
            if (idleDuration.compareTo(timeoutDuration) < 0) {
                break;
            }

            i.remove();
            region.queues.remove(queue.id);
        }
    }



    @Override
    public void close() throws IOException {
        closed = true;
    }
    
    
    
    private LockRegion getLockRegion(String id) {
        int regionIdx = Math.abs(id.hashCode() % regions.length);
        return regions[regionIdx];        
    }


    
    

    private static final class LockRegion {
        private final BestEffortSerializer serializer = new BestEffortSerializer();
        private final HashMap<String, Queue> queues = new HashMap<>();         // actor addr -> stored actor obj
        private final TreeSet<Queue> timeouts = new TreeSet<>((x, y) -> {
            int ret = x.lastAccessTime.compareTo(y.lastAccessTime);
            if (ret == 0 && x != y) { // if we ever encounter the same time (but different objs), treat it as less-than -- we do this
                                      // because we're using this set just to order (we still want duplicates showing up)
                ret = -1;
            }
            return ret;
        }); // timeout -> actor addr
    }
    
    private static final class Queue {
        private final String id;
        private final LinkedList<byte[]> data = new LinkedList<>();
        private Instant lastAccessTime;

        Queue(String id) {
            this.id = id;
        }
        
    }
}
