package com.offbynull.peernetic.playground.unstructuredmesh;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import org.apache.commons.lang3.Validate;

final class AddressCache<A> {
    private final int maxCacheItems;
    private final int minCacheItems;
    private final HashSet<A> cacheSet;
    private final LinkedList<A> cacheList;
    private final RetentionMode retentionMode;
    
    public AddressCache(int max, Collection<A> initial) {
        this(initial.size(), max, initial, RetentionMode.RETAIN_NEWEST);
    }

    public AddressCache(int max, Collection<A> initial, RetentionMode retentionMode) {
        this(initial.size(), max, initial, retentionMode);
    }

    public AddressCache(int min, int max, Collection<A> initial, RetentionMode retentionMode) {
        Validate.noNullElements(initial);
        Validate.isTrue(min >= 0 && max >= 0, "Min/max size must be >= 0");
        Validate.isTrue(initial.size() >= min, "Not enough initial elements to satisfy min size limit");
        Validate.isTrue(initial.size() <= max, "Too many initial elements to satisfy max size limit");
        Validate.notNull(retentionMode);
        
        this.maxCacheItems = max;
        this.minCacheItems = min;
        this.cacheSet = new HashSet<>(initial);
        this.cacheList = new LinkedList<>(initial);
        this.retentionMode = retentionMode;
    }
    
    public void add(A address) {
        Validate.notNull(address);
        if (cacheSet.contains(address)) {
            return;
        }
        
        switch (retentionMode) {
            case RETAIN_NEWEST:
                if (cacheList.size() == maxCacheItems) { // remove isn't available, remove oldest
                    A oldest = cacheList.removeFirst();
                    cacheSet.remove(oldest);
                }
                cacheList.add(address);
                cacheSet.add(address);
                break;
            case RETAIN_OLDEST:
                if (cacheList.size() < maxCacheItems) { // if room is available in cache, add in
                    cacheList.add(address);
                    cacheSet.add(address);
                }
                break;
            default:
                throw new IllegalStateException();
        }
    }

    public void addAll(Collection<A> addresses) {
        Validate.noNullElements(addresses);
        addresses.forEach(x -> add(x));
    }

    public A next() {
        if (cacheList.isEmpty()) {
            return null;
        }
        
        A addr = null;
        switch (retentionMode) {
            case RETAIN_NEWEST:
                addr = cacheList.getFirst();
                if (cacheList.size() - 1 >= minCacheItems) { // if num of items in cache > min, remove item we're returning from cache
                    cacheList.removeFirst();
                    cacheSet.remove(addr);
                } else if (!cacheList.isEmpty()) { // if num of items is <= min, cycle item, so it won't appear next time next() is called
                    A existingAddr = cacheList.removeFirst();
                    cacheList.addLast(existingAddr);
                }
                break;
            case RETAIN_OLDEST:
                addr = cacheList.getLast();
                if (cacheList.size() - 1 >= minCacheItems) { // if num of items in cache > min, remove item we're returning from cache
                    cacheList.removeLast();
                    cacheSet.remove(addr);
                } else if (!cacheList.isEmpty()) { // if num of items is <= min, cycle item, so it won't appear next time next() is called
                    A existingAddr = cacheList.removeLast();
                    cacheList.addFirst(existingAddr);
                }
                break;
            default:
                throw new IllegalStateException();
        }
        
        return addr;
    }
    
    public int size() {
        return cacheList.size();
    }
    
    public boolean isMaximumCapacity() {
        return maxCacheItems == cacheList.size();
    }

    public boolean isMinimumCapacity() {
        return minCacheItems == cacheList.size();
    }
    
    public enum RetentionMode {
        // Attempts to retain the latest addresses put in to the cache, gives back the newest addresses first
        RETAIN_NEWEST,
        // Attempts to retain the oldest addresses put in to the cache, gives back the oldest addresses first
        RETAIN_OLDEST;
    }
}
