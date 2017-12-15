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
import com.offbynull.actors.address.Address;
import com.offbynull.actors.shuttle.Message;
import java.time.Duration;
import java.time.Instant;
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
 * A storage engine that keeps all messages serialized in memory.
 * <p>
 * If the queues for an HTTP client don't get accessed by some user-defined timeout, they are evicted.
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
     * @param prefix prefix for the servlet gateway that this storage engine belongs to
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
    public void queueOut(String id, List<Message> messages) {
        Validate.notNull(id);
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        Validate.validState(!closed, "Store closed");
        messages.forEach(m -> {
            Address dstAddr = m.getDestinationAddress();
            Validate.isTrue(dstAddr.size() >= 2, "HTTP client address must have atleast 2 elements: %s", dstAddr);
            Validate.isTrue(dstAddr.getElement(0).equals(prefix), "HTTP client address must start with %s: %s", prefix, dstAddr);
            Validate.isTrue(dstAddr.getElement(1).equals(id), "HTTP client address must have ID with %s: %s", id, dstAddr);
        });
        
        LockRegion lockRegion = getLockRegion(id);
        synchronized (lockRegion) {
            filterTimedOutContainers(lockRegion);
            
            Queues queue = lockRegion.queues.computeIfAbsent(id, k -> new Queues(id, clock.instant()));
            
            lockRegion.timeouts.remove(queue);
            queue.lastAccessTime = clock.instant();
            lockRegion.timeouts.add(queue);

            List<byte[]> serializedMessages = messages.stream()
                    .map(m -> lockRegion.serializer.serialize(m))
                    .collect(toList());
            queue.outQueue.queue(serializedMessages);
        }
    }

    @Override
    public List<Message> dequeueOut(String id, int offset) {
        Validate.notNull(id);
        Validate.isTrue(offset >= 0);
        Validate.validState(!closed, "Store closed");
        
        LockRegion lockRegion = getLockRegion(id);
        synchronized (lockRegion) {
            filterTimedOutContainers(lockRegion);
            
            Queues queue = lockRegion.queues.computeIfAbsent(id, k -> new Queues(id, clock.instant()));
            
            lockRegion.timeouts.remove(queue);
            queue.lastAccessTime = clock.instant();
            lockRegion.timeouts.add(queue);

            List<byte[]> serializedMessages = queue.outQueue.dequeue(offset);
            List<Message> messages = serializedMessages.stream()
                    .map(d -> (Message) lockRegion.serializer.deserialize(d))
                    .collect(toList());
            
            return messages;
        }
    }

    
    
    
    
    
    @Override
    public void queueIn(String id, int offset, List<Message> messages) {
        Validate.notNull(id);
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        Validate.isTrue(offset >= 0);
        Validate.validState(!closed, "Store closed");
        messages.forEach(m -> {
            Address srcAddr = m.getSourceAddress();
            Validate.isTrue(srcAddr.size() >= 2, "HTTP client address must have atleast 2 elements: %s", srcAddr);
            Validate.isTrue(srcAddr.getElement(0).equals(prefix), "HTTP client address must start with %s: %s", prefix, srcAddr);
            Validate.isTrue(srcAddr.getElement(1).equals(id), "HTTP client address must have ID with %s: %s", id, srcAddr);
        });
        
        LockRegion lockRegion = getLockRegion(id);
        synchronized (lockRegion) {
            filterTimedOutContainers(lockRegion);
            
            Queues queue = lockRegion.queues.computeIfAbsent(id, k -> new Queues(id, clock.instant()));
            
            lockRegion.timeouts.remove(queue);
            queue.lastAccessTime = clock.instant();
            lockRegion.timeouts.add(queue);

            List<byte[]> serializedMessages = messages.stream()
                    .map(m -> lockRegion.serializer.serialize(m))
                    .collect(toList());
            queue.inQueue.queue(offset, serializedMessages);
        }
    }

    @Override
    public List<Message> dequeueIn(String id) {
        Validate.notNull(id);
        Validate.validState(!closed, "Store closed");
        
        LockRegion lockRegion = getLockRegion(id);
        synchronized (lockRegion) {
            filterTimedOutContainers(lockRegion);
            
            Queues queue = lockRegion.queues.computeIfAbsent(id, k -> new Queues(id, clock.instant()));
            
            lockRegion.timeouts.remove(queue);
            queue.lastAccessTime = clock.instant();
            lockRegion.timeouts.add(queue);

            List<byte[]> serializedMessages = queue.inQueue.dequeue();
            List<Message> messages = serializedMessages.stream()
                    .map(d -> (Message) lockRegion.serializer.deserialize(d))
                    .collect(toList());
            
            return messages;
        }
    }
    
    
    


    private void filterTimedOutContainers(LockRegion region) {
        Instant now = clock.instant();

        Iterator<Queues> i = region.timeouts.iterator();
        while (i.hasNext()) {
            Queues queue = i.next();
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
        private final HashMap<String, Queues> queues = new HashMap<>();         // actor addr -> stored actor obj
        private final TreeSet<Queues> timeouts = new TreeSet<>((x, y) -> {
            int ret = x.lastAccessTime.compareTo(y.lastAccessTime);
            if (ret == 0 && x != y) { // if we ever encounter the same time (but different objs), treat it as less-than -- we do this
                                      // because we're using this set just to order (we still want duplicates showing up)
                ret = -1;
            }
            return ret;
        }); // timeout -> actor addr
    }
    
    private static final class Queues {
        private final String id;
        private final OutQueue outQueue = new OutQueue();
        private final InQueue inQueue = new InQueue();
        private Instant lastAccessTime;

        Queues(String id, Instant time) {
            this.id = id;
            this.lastAccessTime = time;
        }
        
    }
    
    
    
    
    private static final class OutQueue {
        private int offset;
        private final LinkedList<byte[]> data = new LinkedList<>();
        
        void queue(List<byte[]> msgs) {
            data.addAll(msgs);
        }
        
        List<byte[]> dequeue(int offset) {
            int idx = offset - this.offset;
            if (idx < 0 || idx > data.size()) {
                throw new IllegalStateException();
            }
            
            data.subList(0, idx).clear(); // dequeue anything before
            this.offset += idx;
            
            return new LinkedList<>(data); // return remainder
        }
    }
    
    private static final class InQueue {
        private int offset;
        private final LinkedList<byte[]> data = new LinkedList<>();
        
        void queue(int offset, List<byte[]> msgs) {
            int tailOffset = this.offset + data.size();
            int tailLen = msgs.size() - (tailOffset - offset);
            
            if (tailLen < 0) {
                return;
            }
            
            if (tailLen > msgs.size()) {
                throw new IllegalStateException();
            }
            
            int msgCropStart = msgs.size() - tailLen;
            int msgCropEnd = msgs.size();
            msgs = msgs.subList(msgCropStart, msgCropEnd);
            data.addAll(msgs);
        }
        
        LinkedList<byte[]> dequeue() {
            LinkedList<byte[]> ret = new LinkedList<>(data);
            
            offset += data.size();
            data.clear();
            
            return ret;
        }
    }
}
