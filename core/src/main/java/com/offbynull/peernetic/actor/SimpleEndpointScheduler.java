package com.offbynull.peernetic.actor;

import java.time.Duration;
import java.time.Instant;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

public final class SimpleEndpointScheduler implements EndpointScheduler {
    private PriorityQueue<Event> events;
    private Lock lock;
    private Condition condition;
    private Thread thread;
    
    public SimpleEndpointScheduler() {
        events = new PriorityQueue<>();
        lock = new ReentrantLock();
        condition = lock.newCondition();
        thread = new Thread(() -> {
            lock.lock();
            try {
                long waitDuration = Long.MAX_VALUE;
                while (true) {
                    if (waitDuration > 0L) {
                        condition.await(waitDuration, TimeUnit.MILLISECONDS);
                    }
                    
                    int counter =0;
                    Event e;
                    Instant currentTime = Instant.now();
                    while ((e = events.peek()) != null && !e.time.isAfter(currentTime)) {
                        events.poll();
                        e.destination.send(e.source, e.message);
                        counter++;
                    }
                    
                    if (e != null) {
                        waitDuration = Duration.between(currentTime, e.time).toMillis();
                    } else {
                        waitDuration = Long.MAX_VALUE;
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                lock.unlock();
            }
        });
        thread.setName(SimpleEndpointScheduler.class.getSimpleName());
        thread.setDaemon(true);
        thread.start();
    }
    
    @Override
    public void scheduleMessage(Duration delay, Endpoint source, Endpoint destination, Object message) {
        Validate.notNull(delay);
        Validate.notNull(source);
        Validate.notNull(destination);
        Validate.notNull(message);
        Validate.isTrue(!delay.isNegative());
        lock.lock();
        try {
            Instant prevFirstTime = events.isEmpty() ? null : events.peek().time;
            
            Event e = new Event(Instant.now().plus(delay), source, destination, message);
            events.add(e);
            
            // If there's a new top-most event in the queue with a shorter hittime, signal thread to wake up
            if (prevFirstTime == null || events.peek().time.isBefore(prevFirstTime)) {
                condition.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws Exception {
        thread.interrupt();
        thread.join();
    }
    
    private static final class Event implements Comparable<Event> {
        private final Instant time;
        private final Endpoint source;
        private final Endpoint destination;
        private final Object message;

        public Event(Instant time, Endpoint source, Endpoint destination, Object message) {
            this.time = time;
            this.source = source;
            this.destination = destination;
            this.message = message;
        }

        @Override
        public int compareTo(Event o) {
            return time.compareTo(o.time);
        }
        
    }
}
