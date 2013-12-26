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

final class OutgoingLinkManager<A> {
    private int maxSize;
    private long staleDuration;
    private long killDuration;
    private Map<A, Entity> addressMap;
    private PriorityQueue<Entity> staleQueue;
    private PriorityQueue<Entity> killQueue;
    
    public OutgoingLinkManager(int maxSize, long staleDuration, long killDuration) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxSize);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, staleDuration);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, killDuration);
        Validate.isTrue(killDuration > staleDuration);

        this.maxSize = maxSize;
        this.staleDuration = staleDuration;
        this.killDuration = killDuration;
        addressMap = new HashMap<>();
        killQueue = new PriorityQueue<>(11, new EntityKillComparator());
        staleQueue = new PriorityQueue<>(11, new EntityStaleComparator());
    }
    
    public boolean addLink(long timestamp, A address, ByteBuffer secret) {
        Validate.notNull(address);
        Validate.notNull(secret);
        
        if (addressMap.size() == maxSize || addressMap.containsKey(address)) {
            return false;
        }
        
        Entity entity = new Entity(address, timestamp + staleDuration, timestamp + killDuration, secret);
        addressMap.put(address, entity);
        
        return true;
    }

    public boolean removeLink(A address) {
        Validate.notNull(address);
        
        Entity entity;
        if ((entity = addressMap.get(address)) == null) {
            return false;
        }

        addressMap.remove(address);
        entity.ignore();
        
        return true;
    }
    
    public boolean updateLink(long timestamp, A address) {
        Validate.notNull(address);
        
        Entity entity;
        if ((entity = addressMap.get(address)) == null) {
            return false;
        }
        entity.ignore();
        
        Entity newEntity = new Entity(address, timestamp + staleDuration, timestamp + killDuration, entity.getSecret());
        staleQueue.add(newEntity);
        
        return true;
    }
    
    public ProcessResult<A> process(long timestamp) {
        Map<A, ByteBuffer> staleAddresses = new HashMap<>();
        Entity staleEntity;
        while ((staleEntity = killQueue.peek()) != null) {
            if (staleEntity.isIgnore()) {
                continue;
            }
            
            if (staleEntity.getStaleTime() <= timestamp) {
                staleQueue.poll();
                killQueue.add(staleEntity);
                
                staleAddresses.put(staleEntity.getAddress(), staleEntity.getSecret());
            } else {
                break;
            }
        }
        
        Set<A> killedAddresses = new HashSet<>();
        Entity killEntity;
        while ((killEntity = killQueue.peek()) != null) {
            if (killEntity.isIgnore()) {
                continue;
            }
            
            if (killEntity.getKillTime() <= timestamp) {
                killQueue.poll();
                addressMap.remove(killEntity.getAddress());
                
                killedAddresses.add(killEntity.getAddress());
            } else {
                break;
            }
        }
        
        long waitUntil = Math.min(staleEntity == null ? Long.MAX_VALUE : staleEntity.getStaleTime(),
                killEntity == null ? Long.MAX_VALUE : killEntity.getKillTime());
        
        return new ProcessResult<>(waitUntil, staleAddresses, killedAddresses);
    }
    
    public Set<A> getLinks() {
        return new HashSet<>(addressMap.keySet());
    }
    
    public int getRemaining() {
        return addressMap.size() - maxSize;
    }
    
    public static final class ProcessResult<A> {
        private long waitUntil;
        private Map<A, ByteBuffer> staleAddresses;
        private Set<A> killedAddresses;

        private ProcessResult(long waitUntil, Map<A, ByteBuffer> staleAddresses, Set<A> killedAddresses) {
            this.waitUntil = waitUntil;
            this.staleAddresses = Collections.unmodifiableMap(staleAddresses);
            this.killedAddresses = Collections.unmodifiableSet(killedAddresses);
        }

        public long getWaitUntil() {
            return waitUntil;
        }

        public Map<A, ByteBuffer> getStaleAddresses() {
            return staleAddresses;
        }

        public Set<A> getKilledAddresses() {
            return killedAddresses;
        }
        
    }
    
    private final class EntityKillComparator implements Comparator<Entity> {

        @Override
        public int compare(Entity o1, Entity o2) {
            return Long.compare(o1.getKillTime(), o2.getKillTime());
        }
        
    }
    
    private final class EntityStaleComparator implements Comparator<Entity> {

        @Override
        public int compare(Entity o1, Entity o2) {
            return Long.compare(o1.getStaleTime(), o2.getStaleTime());
        }
        
    }
    
    private final class Entity {
        private A address;
        private long staleTime;
        private long killTime;
        private boolean ignore;
        private ByteBuffer secret;

        public Entity(A address, long staleTime, long killTime, ByteBuffer secret) {
            this.address = address;
            this.staleTime = staleTime;
            this.killTime = killTime;
            this.secret = ByteBuffer.allocate(secret.remaining());
            this.secret.put(secret);
            this.secret.flip();
        }

        public A getAddress() {
            return address;
        }

        public long getStaleTime() {
            return staleTime;
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

        public void updateStaleTime(long staleTime) {
            this.staleTime = staleTime;
        }
        
        public void updateKillTime(long killTime) {
            this.killTime = killTime;
        }
        
        public boolean isIgnore() {
            return ignore;
        }
    }
}
