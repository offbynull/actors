package com.offbynull.rpc.transport.fake;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

/**
 * A hub that pipes messages between {@link FakeTransport}s.
 * @author Kasra F
 * @param <A> address type
 */
public final class FakeHub<A> {

    private PriorityQueue<Message<A>> transitPacketQueue;
    private Map<A, FakeEndpoint<A>> addressMap;
    private Line<A> line;
    

    private EventLoop eventLoop;
    private Thread eventLoopThread;

    private Lock lock;
    private Condition transitSleepCondition;
    private Condition startedCondition;
    private Condition stoppedCondition;
    
    State state;

    /**
     * Construct a {@link FakeHub} object.
     * @param line line to use
     * @throws NullPointerException if any arguments are {@code null}
     */
    public FakeHub(Line<A> line) {
        Validate.notNull(line);

        transitPacketQueue = new PriorityQueue<>(11, new MessageArriveTimeComparator());
        addressMap = new HashMap<>();
        this.line = line;

        lock = new ReentrantLock();
        transitSleepCondition = lock.newCondition();
        startedCondition = lock.newCondition();
        stoppedCondition = lock.newCondition();

        eventLoop = new EventLoop();
        eventLoopThread = new Thread(eventLoop, "Fake Event Loop");
        
        state = State.UNKNOWN;
    }

    /**
     * Start.
     * @throws IOException on error
     * @throws IllegalStateException if already started
     */
    public void start() throws IOException {
        lock.lock();
        try {
            Validate.validState(state == State.UNKNOWN);
            
            eventLoopThread.start();
            startedCondition.awaitUninterruptibly();
            
            state = State.STARTED;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stop.
     * @throws IllegalStateException if not started
     */
    public void stop() {
        lock.lock();
        try {
            Validate.validState(state == State.STARTED);
            
            eventLoop.triggerShutdown();
            stoppedCondition.awaitUninterruptibly();
            
            state = State.STOPPED;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Add an endpoint to the hub.
     * @param address endpoint address
     * @param receiver receiver that receives messages from the hub to {@code address}
     * @return an object to send messages to the hub from {@code address}
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalStateException if not started
     */
    FakeHubSender<A> addEndpoint(A address, FakeHubReceiver<A> receiver) {
        Validate.notNull(address);
        Validate.notNull(receiver);

        lock.lock();
        try {
            Validate.validState(state == State.STARTED);
            Validate.isTrue(!addressMap.containsKey(address));
            addressMap.put(address, new FakeEndpoint<>(receiver));
            
            return new FakeHubSender<>(lock, transitSleepCondition, transitPacketQueue, addressMap, line);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove an endpoint from the hub.
     * @param address endpoint address
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalStateException if not started
     */
    void removeEndpoint(A address) {
        Validate.notNull(address);

        lock.lock();
        try {
            Validate.validState(state == State.STARTED);
            addressMap.remove(address);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Activates an endpoint from the hub. Call this after adding.
     * @param address endpoint address
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalStateException if not started
     * @throws IllegalArgumentException if {@code address} does not exist as an endpoint on the hub
     */
    void activateEndpoint(A address) {
        Validate.notNull(address);

        lock.lock();
        try {
            Validate.validState(state == State.STARTED);
            Validate.isTrue(addressMap.containsKey(address));
            addressMap.get(address).setActive(true);
        } finally {
            lock.unlock();
        }
    }

    private final class EventLoop implements Runnable {

        private boolean stop;


        @Override
        public void run() {
            lock.lock();
            try {
                startedCondition.signal();
                
                while (true) {
                    if (stop) {
                        break;
                    }
                    
                    long time = System.currentTimeMillis();

                    List<Message<A>> packets = new LinkedList<>();
                    FakeEndpoint<A> dest;

                    if (transitPacketQueue.isEmpty()) {
                        try {
                            transitSleepCondition.await();
                        } catch (InterruptedException ex) {
                            break;
                        }
                        continue;
                    } else {
                        Message<A> topPacket = transitPacketQueue.peek();
                        
                        long topClosestArriveTime = topPacket.getArriveTime();
                        long topWaitTime = topClosestArriveTime - time;

                        if (topWaitTime > 0L) {
                            try {
                                transitSleepCondition.await(topWaitTime, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException ex) {
                                break;
                            }
                            continue;
                        }
                        
                        
                        Message<A> packet;
                        while ((packet = transitPacketQueue.peek()) != null) {
                            long closestArriveTime = packet.getArriveTime();
                            long waitTime = closestArriveTime - time;                            
                            
                            if (waitTime <= 0L) {
                                packets.add(packet);
                                transitPacketQueue.poll(); // remove
                            }
                        }
                    }

                    
                    for (Message<A> packet : packets) {
                        dest = addressMap.get(packet.getTo());
                        if (dest == null || !dest.isActive()) {
                            continue;
                        }

                        try {
                            dest.getReceiver().incoming(packet);
                        } catch (RuntimeException re) {
                            // do nothing
                        }
                    }
                    
                    line.arrive(packets);
                }
                
                stoppedCondition.signal();
            } finally {
                lock.unlock();
            }
        }

        public void triggerShutdown() {
            lock.lock();
            try {
                stop = true;
                transitSleepCondition.signal();
            } finally {
                lock.unlock();
            }
        }
    }
    
    private enum State {
        UNKNOWN,
        STARTED,
        STOPPED
    }
}
