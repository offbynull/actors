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
package com.offbynull.peernetic.overlay.unstructured;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.apache.commons.lang3.Validate;

final class IncomingLinkManager<A> {
    private int maxSize;
    private long killDuration;
    private Map<A, Entity> addressMap;
    private PriorityQueue<Entity> killQueue;
    
    public IncomingLinkManager(int maxSize, long killDuration) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxSize);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, killDuration);

        this.maxSize = maxSize;
        this.killDuration = killDuration;
        addressMap = new HashMap<>();
        killQueue = new PriorityQueue<>(11, new EntityComparator());
    }
    
    public boolean addLink(long timestamp, A address, ByteBuffer secret) {
        Validate.notNull(address);
        Validate.notNull(secret);
        
        if (addressMap.size() == maxSize || addressMap.containsKey(address)) {
            return false;
        }
        
        Entity entity = new Entity(address, timestamp + killDuration, secret);
        addressMap.put(address, entity);
        
        return true;
    }
    
    public boolean updateLink(long timestamp, A address, ByteBuffer secret) {
        Validate.notNull(address);
        Validate.notNull(secret);
        
        Entity entity;
        if ((entity = addressMap.get(address)) == null) {
            return false;
        }
        
        if (!secret.equals(entity.getSecret())) {
            return false;
        }
        
        entity.updateKillTime(timestamp + killDuration);
        
        return true;
    }

    public boolean removeLink(long timestamp, A address, ByteBuffer secret) {
        Validate.notNull(address);
        Validate.notNull(secret);
        
        Entity entity;
        if ((entity = addressMap.get(address)) == null) {
            return false;
        }
        
        if (!secret.equals(entity.getSecret())) {
            return false;
        }
        
        addressMap.remove(address);
        entity.ignore();
        
        return true;
    }
    
    public Set<A> getLinks() {
        return new HashSet<>(addressMap.keySet());
    }

    public int getFreeSlots() {
        return maxSize - addressMap.size();
    }
    
    public ProcessResult<A> process(long timestamp) {
        Set<A> killedAddresses = new HashSet<>();
        Entity entity;
        while ((entity = killQueue.peek()) != null) {
            if (entity.isIgnore()) {
                continue;
            }
            
            if (entity.getKillTime() <= timestamp) {
                killQueue.poll();
                addressMap.remove(entity.getAddress());
                
                killedAddresses.add(entity.getAddress());
            } else {
                break;
            }
        }
        
        long waitUntil = entity == null ? Long.MAX_VALUE : entity.getKillTime();
        
        return new ProcessResult<>(waitUntil, killedAddresses);
    }
    
    public static final class ProcessResult<A> {
        private long waitUntil;
        private Set<A> killedAddresses;

        private ProcessResult(long waitUntil, Set<A> killedAddresses) {
            this.waitUntil = waitUntil;
            this.killedAddresses = Collections.unmodifiableSet(killedAddresses);
        }

        public long getWaitUntil() {
            return waitUntil;
        }

        public Set<A> getKilledAddresses() {
            return killedAddresses;
        }
        
    }
    
    private class EntityComparator implements Comparator<Entity> {

        @Override
        public int compare(Entity o1, Entity o2) {
            return Long.compare(o1.getKillTime(), o2.getKillTime());
        }
        
    }
    
    private class Entity {
        private A address;
        private long killTime;
        private boolean ignore;
        private ByteBuffer secret;

        public Entity(A address, long killTime, ByteBuffer secret) {
            this.address = address;
            this.killTime = killTime;
            this.secret = ByteBuffer.allocate(secret.remaining());
            this.secret.put(secret);
            this.secret.flip();
        }

        public A getAddress() {
            return address;
        }

        public long getKillTime() {
            return killTime;
        }

        public ByteBuffer getSecret() {
            return secret.asReadOnlyBuffer();
        }
        
        public void ignore() {
            ignore = true;
        }
        
        public void updateKillTime(long killTime) {
            this.killTime = killTime;
        }
        
        public boolean isIgnore() {
            return ignore;
        }
        
    }
}
