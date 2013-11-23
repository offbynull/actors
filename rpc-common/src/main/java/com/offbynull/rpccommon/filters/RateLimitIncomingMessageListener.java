package com.offbynull.rpccommon.filters;

import com.offbynull.rpc.transport.IncomingMessage;
import com.offbynull.rpc.transport.IncomingMessageListener;
import com.offbynull.rpc.transport.IncomingMessageResponseHandler;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

public final class RateLimitIncomingMessageListener<A> implements IncomingMessageListener<A> {
    private int maxHit;
    private long maxDuration;
    private HashSet<A> bannedLookup;
    private PriorityQueue<State> durationQueue;
    private HashMap<A, State> addressLookup;
    
    private Lock lock;

    public RateLimitIncomingMessageListener(int maxHit, long maxDuration) {
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
    public void messageArrived(IncomingMessage<A> message, IncomingMessageResponseHandler responseCallback) {
        lock.lock();
        
        try {
            A from = message.getFrom();

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
            if (state.getHitCount() >= maxHit) {
                state.ignore();
                addressLookup.remove(from);
                bannedLookup.add(from);
            }
        } finally {
            lock.unlock();
        }
    }
    
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
            if (state.isIgnore() || state.getStartTime() + maxDuration >= currentTime) {
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

    private static final class StateComparator implements Comparator<State> {

        @Override
        public int compare(State o1, State o2) {
            return Long.compare(o1.getStartTime(), o2.getStartTime());
        }
        
    }
    
    public static class AddressBannedException extends RuntimeException {
    }
}
