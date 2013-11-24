package com.offbynull.rpc.transport.fake;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import org.apache.commons.lang3.Validate;

public final class FakeHubSender<A> {
    private Lock lock;
    private Condition condition;
    private PriorityQueue<Packet<A>> transitPacketQueue;
    private Map<A, FakeEndpoint<A>> addressMap;
    private Line<A> line;

    FakeHubSender(Lock lock, Condition condition, PriorityQueue<Packet<A>> transitPacketQueue, Map<A, FakeEndpoint<A>> addressMap,
            Line<A> line) {
        Validate.notNull(lock);
        Validate.notNull(condition);
        Validate.notNull(transitPacketQueue);
        Validate.notNull(addressMap);
        Validate.notNull(line);
        
        this.lock = lock;
        this.condition = condition;
        this.transitPacketQueue = transitPacketQueue;
        this.addressMap = addressMap;
        this.line = line;
    }


    public void send(A from, A to, ByteBuffer data) {
        Validate.notNull(from);
        Validate.notNull(to);
        Validate.notNull(data);

        lock.lock();
        try 
        {
            FakeEndpoint<A> endpoint = addressMap.get(from);
            if (endpoint == null || !endpoint.isActive()) {
                return;
            }
            
            List<Packet<A>> result = line.queue(from, to, data);
            
            transitPacketQueue.addAll(result);
            condition.signal();
            System.out.println("signalled");
        } finally {
            lock.unlock();
        }
    }
}
