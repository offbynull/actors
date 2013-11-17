package com.offbynull.p2prpc.transport.udp;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.LinkedList;
import org.apache.commons.lang3.Validate;

final class MessageIdCache {

    private int capacity;
    private LinkedList<MessageIdInstance> queue;
    private HashSet<MessageIdInstance> set;

    public MessageIdCache(int capacity) {
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, capacity);

        this.capacity = capacity;
        this.queue = new LinkedList<>();
        this.set = new HashSet<>(capacity);
    }

    public boolean add(InetSocketAddress from, MessageId id, PacketType type) {
        MessageIdInstance idInstance = new MessageIdInstance(from, id, type);

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
    }
}
