package com.offbynull.p2prpc.transport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public final class TimeoutTracker<T> {
    private PriorityQueue<Entry> queue;
    private Map<T, Entry> entryLookup;
    
    public TimeoutTracker() {
        queue = new PriorityQueue<>();
        entryLookup = new HashMap<>();
    }
    
    public void add(T entity, long killTime) {
        queue.add(new Entry(entity, killTime));
    }
    
    public void remove(T entity) {
        Entry entry = entryLookup.get(entity);
        if (entry != null) {
            entry.ignore();
        }
    }
    
    public Collection<T> getTimedOut(long currentTime) {
        List<T> ret = new ArrayList<>();

        Entry entry;
        while ((entry = queue.peek()) != null) {
            if (entry.isIgnored()) {
                queue.poll();
                entryLookup.remove(entry.getObject());
            } else if (currentTime >= entry.getKillTime()) {
                T entity = entry.getObject();
                
                queue.poll();
                entryLookup.remove(entity);
                ret.add(entity);
            }
        }

        return ret;
    }
    
    public long getNextQueryTime() {
        Entry entry;
        while ((entry = queue.peek()) != null) {
            if (entry.isIgnored()) {
                queue.poll();
                entry = null;
            }
        }
        
        return entry == null ? Long.MAX_VALUE : entry.getKillTime();
    }
    
    private final class Entry implements Comparable<Entry> {
        private T object;
        private long killTime;
        private boolean ignored;

        public Entry(T object, long killTime) {
            this.object = object;
            this.killTime = killTime;
        }

        public T getObject() {
            return object;
        }

        public long getKillTime() {
            return killTime;
        }

        public boolean isIgnored() {
            return ignored;
        }

        public void ignore() {
            ignored = true;
        }
        
        @Override
        public int compareTo(Entry o) {
            return Long.compare(killTime, o.killTime);
        }
        
    }
}
