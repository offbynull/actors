/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.nettyp2p.handlers.ratelimit;

import com.offbynull.peernetic.nettyp2p.handlers.common.AbstractFilterHandler;
import java.io.Serializable;
import java.net.SocketAddress;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

/**
 * A Netty handler that checks to make sure a certain address isn't hammering us with messages.
 *
 * @author Kasra Faghihi
 */
public final class RateLimitCheckHandler extends AbstractFilterHandler {

    private int maxHit;
    private long maxDuration;
    private HashSet<SocketAddress> bannedLookup;
    private PriorityQueue<State> durationQueue;
    private HashMap<SocketAddress, State> addressLookup;

    private Lock lock;

    /**
     * Constructs a {@link RateLimitIncomingFilter} object.
     *
     * @param maxHit maximum number of hits allowed within {@link duration}
     * @param maxDuration duration of time before the hit counter for an address resets
     * @param closeChannelOnFailure close channel if we are being hammered
     * @throws IllegalArgumentException if either argument is {@code <= 0}
     */
    public RateLimitCheckHandler(int maxHit, long maxDuration, boolean closeChannelOnFailure) {
        super(closeChannelOnFailure);

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
    protected boolean filter(SocketAddress local, SocketAddress remote, Object content, Trigger trigger) throws Exception {
        switch (trigger) {
            case CONNECTION:
            case WRITE:
                return false;
            default:
                break;
        }

        lock.lock();

        try {
            if (bannedLookup.contains(remote)) {
                throw new AddressBannedException();
            }

            long time = System.currentTimeMillis();

            clearExceededDurations(time);

            State state = addressLookup.get(remote);
            if (state == null) {
                state = new State(time, remote);
                addressLookup.put(remote, state);
                durationQueue.add(state);
            }

            state.incrementHitCount();
            if (state.getHitCount() > maxHit) {
                state.ignore();
                addressLookup.remove(remote);
                bannedLookup.add(remote);
                return true;
            }

            return false;
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
        State state;
        while ((state = durationQueue.peek()) != null) {
            if (state.isIgnore() || state.getStartTime() + maxDuration <= currentTime) {
                durationQueue.poll();
                addressLookup.remove(state.getAddress());
            } else {
                break;
            }
        }
    }

    private static final class State {

        private long startTime;
        private int hitCount;
        private SocketAddress address;
        private boolean ignore;

        public State(long startTime, SocketAddress address) {
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

        public SocketAddress getAddress() {
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
