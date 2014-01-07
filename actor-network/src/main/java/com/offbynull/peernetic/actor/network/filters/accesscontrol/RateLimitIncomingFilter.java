/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.actor.network.filters.accesscontrol;

import com.offbynull.peernetic.actor.network.IncomingFilter;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

/**
 * An {@link IncomingFilter} that ensures a certain address isn't hammering.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class RateLimitIncomingFilter<A> implements IncomingFilter<A> {
    private int maxHit;
    private long maxDuration;
    private HashSet<A> bannedLookup;
    private PriorityQueue<State> durationQueue;
    private HashMap<A, State> addressLookup;
    
    private Lock lock;

    /**
     * Constructs a {@link RateLimitIncomingFilter} object.
     * @param maxHit maximum number of hits allowed within {@link duration}
     * @param maxDuration duration of time before the hit counter for an address resets
     * @throws IllegalArgumentException if either argument is {@code <= 0}
     */
    public RateLimitIncomingFilter(int maxHit, long maxDuration) {
        Validate.exclusiveBetween(1L, Long.MAX_VALUE, maxDuration);
        Validate.exclusiveBetween(1, Integer.MAX_VALUE, maxHit);
        this.maxHit = maxHit;
        this.maxDuration = maxDuration;
        this.durationQueue = new PriorityQueue<>(11, new StateComparator());
        this.addressLookup = new HashMap<>();
        this.bannedLookup = new HashSet<>();
        
        this.lock = new ReentrantLock();
    }

    @Override
    public ByteBuffer filter(A from, ByteBuffer buffer) {
        lock.lock();
        
        try {
            if (bannedLookup.contains(from)) {
                throw new AddressBannedException();
            }

            long time = System.currentTimeMillis();

            clearExceededDurations(time);

            State<A> state = addressLookup.get(from);
            if (state == null) {
                state = new State(time, from);
                addressLookup.put(from, state);
                durationQueue.add(state);
            }

            state.incrementHitCount();
            if (state.getHitCount() > maxHit) {
                state.ignore();
                addressLookup.remove(from);
                bannedLookup.add(from);
                throw new AddressBannedException();
            }
            
            return buffer;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Clear banned addresses.
     */
    public void clearBanned() {
        lock.lock();
        
        try {
            bannedLookup.clear();
        } finally {
            lock.unlock();
        }
    }

    private void clearExceededDurations(long currentTime) {
        State<A> state;
        while ((state = durationQueue.peek()) != null) {
            if (state.isIgnore() || state.getStartTime() + maxDuration <= currentTime) {
                durationQueue.poll();
                addressLookup.remove(state.getAddress());
            } else {
                break;
            }
        }
    }

    private static final class State<A> {
        private long startTime;
        private int hitCount;
        private A address;
        private boolean ignore;

        public State(long startTime, A address) {
            this.startTime = startTime;
            this.address = address;
        }
        
        public void incrementHitCount() {
            hitCount++;
        }

        public long getStartTime() {
            return startTime;
        }

        public int getHitCount() {
            return hitCount;
        }

        public A getAddress() {
            return address;
        }

        public boolean isIgnore() {
            return ignore;
        }
        
        public void ignore() {
            ignore = true;
        }
    }

    private static final class StateComparator implements Comparator<State>, Serializable {
        
        private static final long serialVersionUID = 0L;
        
        @Override
        public int compare(State o1, State o2) {
            return Long.compare(o1.getStartTime(), o2.getStartTime());
        }
        
    }
    
    /**
     * Exception thrown when {@link RateLimitIncomingFilter#filter(java.lang.Object, java.nio.ByteBuffer) } notices that the message belongs
     * to an address that's been hammering.
     */
    public static class AddressBannedException extends RuntimeException {
    }
}
