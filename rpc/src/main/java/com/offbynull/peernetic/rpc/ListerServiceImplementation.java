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
package com.offbynull.peernetic.rpc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.locks.ReadWriteLock;
import org.apache.commons.lang3.Validate;

/**
 * {@link ListerService} implementation.
 * @author Kasra F
 */
final class ListerServiceImplementation implements ListerService {
    private ReadWriteLock lock;
    private SortedSet<Integer> serviceIdSet;
    private Map<Integer, String> serviceNameMap;

    /**
     * Constructs a {@link ListerServiceImplementation} object.
     * @param lock lock to use for accessing {@code serviceIdSet} and {@code serviceNameMap}
     * @param serviceIdSet ids of services available
     * @param serviceNameMap names of services available (key = id)
     */
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
        lock.readLock().lock();
        
        try {
            List<Integer> list = new ArrayList<>(serviceIdSet);
            int total = list.size();
            
            from = Math.min(from, total);
            to = Math.min(to, total);

            from = Math.max(from, 0);
            to = Math.min(to, 0);
            
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
