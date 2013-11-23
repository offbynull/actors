package com.offbynull.rpc.common.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.locks.ReadWriteLock;
import org.apache.commons.lang3.Validate;

public final class ListerServiceImplementation implements ListerService {
    private ReadWriteLock lock;
    private SortedSet<Integer> serviceIdSet;
    private Map<Integer, String> serviceNameMap;

    public ListerServiceImplementation(ReadWriteLock lock, SortedSet<Integer> serviceIdSet, Map<Integer, String> serviceNameMap) {
        Validate.notNull(lock);
        Validate.notNull(serviceIdSet);
        Validate.notNull(serviceNameMap);
        
        this.lock = lock;
        this.serviceIdSet = serviceIdSet;
        this.serviceNameMap = serviceNameMap;
    }

    @Override
    public Services listServices(int from, int to) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, from);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, to);
        Validate.isTrue(from <= to);
        
        lock.readLock().lock();
        
        try {
            List<Integer> list = new ArrayList<>(serviceIdSet);
            int total = list.size();
            
            from = Math.min(from, total);
            to = Math.min(to, total);
            
            list.subList(from, to);
            
            return new Services(total, list);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String getServiceName(int id) {
        lock.readLock().lock();
        
        try {
            return serviceNameMap.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }
}
