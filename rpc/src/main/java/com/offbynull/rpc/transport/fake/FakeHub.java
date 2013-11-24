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

public final class FakeHub<A> {

    private PriorityQueue<Packet<A>> transitPacketQueue;
    private Map<A, FakeEndpoint<A>> addressMap;
    private Line<A> line;
    

    private EventLoop eventLoop;
    private Thread eventLoopThread;

    private Lock lock;
    private Condition transitSleepCondition;
    private Condition startedCondition;
    private Condition stoppedCondition;
    
    State state;

    public FakeHub(Line<A> line) {
        Validate.notNull(line);

        transitPacketQueue = new PriorityQueue<>();
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

    public FakeHubSender<A> addEndpoint(A address, FakeHubReceiver<A> receiver) {
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

    public void removeEndpoint(A address) {
        Validate.notNull(address);

        lock.lock();
        try {
            Validate.validState(state == State.STARTED);
            addressMap.remove(address);
        } finally {
            lock.unlock();
        }
    }

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

                    List<Packet<A>> packets = new LinkedList<>();
                    FakeEndpoint<A> dest;

                    if (transitPacketQueue.isEmpty()) {
                        System.out.println("wait 1");
                        try {
                            transitSleepCondition.await();
                        } catch (InterruptedException ex) {
                            break;
                        }
                        System.out.println("work up " + stop);
                        continue;
                    } else {
                        Packet<A> topPacket = transitPacketQueue.peek();
                        
                        long topClosestArriveTime = topPacket.getArriveTime();
                        long topWaitTime = topClosestArriveTime - time;

                        if (topWaitTime > 0L) {
                            System.out.println("wait 2");
                            try {
                                transitSleepCondition.await(topWaitTime, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException ex) {
                                break;
                            }
                            System.out.println("work up " + stop);
                            continue;
                        }
                        
                        
                        Packet<A> packet;
                        while ((packet = transitPacketQueue.peek()) != null) {
                            long closestArriveTime = packet.getArriveTime();
                            long waitTime = closestArriveTime - time;                            
                            
                            if (waitTime <= 0L) {
                                packets.add(packet);
                                transitPacketQueue.poll(); // remove
                            }
                        }
                    }

                    
                    for (Packet<A> packet : packets) {
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
                    
                    line.unqueue(packets);
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
                System.out.println("signalled");
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
