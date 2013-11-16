package com.offbynull.p2prpc.session;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

public final class MessageIdCache<A> {
    private int capacity;
    private LinkedList<MessageIdInstance> queue;
    private HashSet<MessageIdInstance> set;
    private Lock lock;

    public MessageIdCache(int capacity) {
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, capacity);
        
        this.capacity = capacity;
        this.queue = new LinkedList<>();
        this.set = new HashSet<>(capacity);
        lock = new ReentrantLock();
    }
    
    public boolean add(A from, MessageId id) {
        MessageIdInstance idInstance = new MessageIdInstance(from, id);
        
        lock.lock();
        try {
            if (set.contains(idInstance)) {
                return false;
            }

            if (queue.size() == capacity) {
                MessageIdInstance last = queue.removeLast();
                set.remove(last);
            }

            queue.addFirst(idInstance);
            set.add(idInstance);

            return true;
        } finally {
            lock.unlock();
        }
    }
    
    private final class MessageIdInstance {
        private A from;
        private MessageId id;

        public MessageIdInstance(A from, MessageId id) {
            Validate.notNull(from);
            Validate.notNull(id);
            
            this.from = from;
            this.id = id;
        }

        public A getFrom() {
            return from;
        }

        public MessageId getId() {
            return id;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 23 * hash + Objects.hashCode(this.from);
            hash = 23 * hash + Objects.hashCode(this.id);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MessageIdInstance other = (MessageIdInstance) obj;
            if (!Objects.equals(this.from, other.from)) {
                return false;
            }
            if (!Objects.equals(this.id, other.id)) {
                return false;
            }
            return true;
        }
        
    }
}
