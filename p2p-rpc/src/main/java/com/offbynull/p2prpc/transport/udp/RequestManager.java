package com.offbynull.p2prpc.transport.udp;

import com.offbynull.p2prpc.transport.OutgoingMessageResponseListener;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class RequestManager<A> {
    private long timeout;
    private HashMap<MessageIdInstance<A>, Entity> messageIdSet;
    private LinkedList<Entity> idQueue;

    public RequestManager(long timeout) {
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);
        
        this.timeout = timeout;
        messageIdSet = new HashMap<>();
        idQueue = new LinkedList<>();
    }
    
    public void addRequestId(A dest, MessageId id, OutgoingMessageResponseListener<InetSocketAddress> receiver, long currentTime) {
        MessageIdInstance<A> idInstance = new MessageIdInstance<>(dest, id);
        Entity entity = new Entity(currentTime + timeout, idInstance, receiver);
        
        messageIdSet.put(idInstance, entity);
        idQueue.addLast(entity);
    }

    public OutgoingMessageResponseListener<InetSocketAddress> getReceiver(A dest, MessageId id) {
        MessageIdInstance<A> idInstance = new MessageIdInstance<>(dest, id);
        
        Entity entity = messageIdSet.get(idInstance);
        
        return entity == null ? null : entity.getReceiver();
    }
    
    public Result evaluate(long currentTime) {
        List<MessageId> timedOutIds = new LinkedList<>();
        long waitDuration = 0L;
        
        while (true) {
            Entity entity = idQueue.peekFirst();
            
            if (entity == null) {
                break;
            }
            
            if (currentTime >= entity.getTimeoutTimestamp()) {
                timedOutIds.add(entity.getInstance().getId());
                idQueue.pollFirst();
            } else {
                waitDuration = entity.getTimeoutTimestamp() - currentTime;
                if (waitDuration <= 0L) {
                    waitDuration = 1L;
                }
            }
        }
        
        return new Result(timedOutIds, waitDuration);
    }
    
    public static final class Result {
        private Collection<MessageId> timedOutIds;
        private long waitDuration;

        public Result(Collection<MessageId> timedOutIds, long waitDuration) {
            this.timedOutIds = timedOutIds;
            this.waitDuration = waitDuration;
        }

        public Collection<MessageId> getTimedOutIds() {
            return timedOutIds;
        }

        public long getWaitDuration() {
            return waitDuration;
        }
        
    }
    
    private static final class Entity {
        private long timeoutTimestamp;
        private OutgoingMessageResponseListener<InetSocketAddress> receiver;
        private MessageIdInstance instance;

        public Entity(long timeoutTimestamp, MessageIdInstance instance, OutgoingMessageResponseListener<InetSocketAddress> receiver) {
            this.timeoutTimestamp = timeoutTimestamp;
            this.instance = instance;
            this.receiver = receiver;
        }

        public long getTimeoutTimestamp() {
            return timeoutTimestamp;
        }

        public MessageIdInstance getInstance() {
            return instance;
        }

        public OutgoingMessageResponseListener<InetSocketAddress> getReceiver() {
            return receiver;
        }
        
    }
}
