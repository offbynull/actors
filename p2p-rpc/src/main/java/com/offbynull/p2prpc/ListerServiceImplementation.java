package com.offbynull.p2prpc;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.locks.ReadWriteLock;
import org.apache.commons.lang3.Validate;

final class ListerServiceImplementation implements ListerService {
    private ReadWriteLock lock;
    private SortedSet<Integer> serviceSet;

    public ListerServiceImplementation(ReadWriteLock lock, SortedSet<Integer> serviceSet) {
        Validate.notNull(lock);
        Validate.notNull(serviceSet);
        
        this.lock = lock;
        this.serviceSet = serviceSet;
    }

    @Override
    public Response query(int from, int to) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, from);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, to);
        Validate.isTrue(from <= to);
        
        lock.readLock().lock();
        
        try {
            List<Integer> list = new ArrayList<>(serviceSet);
            int total = list.size();
            
            from = Math.min(from, total);
            to = Math.min(to, total);
            
            list.subList(from, to);
            
            return new Response(total, list);
        } finally {
            lock.readLock().unlock();
        }
    }
}
