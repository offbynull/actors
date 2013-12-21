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
package com.offbynull.rpc.transport.udp;

import com.offbynull.rpc.transport.OutgoingMessageResponseListener;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.apache.commons.lang3.Validate;

final class TimeoutManager {
    private long timeout;
    private HashMap<MessageIdInstance, Entity> messageIdSet;
    private LinkedList<Entity> idQueue;

    public TimeoutManager(long timeout) {
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);
        
        this.timeout = timeout;
        messageIdSet = new HashMap<>();
        idQueue = new LinkedList<>();
    }
    
    public void addRequestId(InetSocketAddress dest, MessageId id, OutgoingMessageResponseListener<InetSocketAddress> receiver,
            long currentTime) {
        Validate.notNull(dest);
        Validate.notNull(id);
        Validate.notNull(receiver);
        
        MessageIdInstance idInstance = new MessageIdInstance(dest, id, PacketType.REQUEST);
        
        Validate.isTrue(!messageIdSet.containsKey(idInstance));
        
        Entity entity = new Entity(currentTime + timeout, idInstance, receiver);
        
        messageIdSet.put(idInstance, entity);
        idQueue.addLast(entity);
    }

    public OutgoingMessageResponseListener<InetSocketAddress> getReceiver(InetSocketAddress dest, MessageId id) {
        Validate.notNull(dest);
        Validate.notNull(id);
        
        MessageIdInstance idInstance = new MessageIdInstance(dest, id, PacketType.REQUEST);
        
        Entity entity = messageIdSet.get(idInstance);
        
        return entity == null ? null : entity.getReceiver();
    }
    
    public Result pending() {
        Set<OutgoingMessageResponseListener<InetSocketAddress>> timedOutReceivers = new HashSet<>();
        
        for (Entity entity : idQueue) {
            timedOutReceivers.add(entity.getReceiver());
        }
        
        return new Result(timedOutReceivers, 0L);
    }
    
    public Result evaluate(long currentTime) {
        Set<OutgoingMessageResponseListener<InetSocketAddress>> timedOutReceivers = new HashSet<>();
        long waitDuration = 0L;
        
        while (true) {
            Entity entity = idQueue.peekFirst();
            
            if (entity == null) {
                break;
            }
            
            if (currentTime >= entity.getTimeoutTimestamp()) {
                timedOutReceivers.add(entity.getReceiver());
                idQueue.pollFirst();
            } else {
                waitDuration = entity.getTimeoutTimestamp() - currentTime;
                if (waitDuration <= 0L) {
                    waitDuration = 1L;
                }
                break;
            }
        }
        
        return new Result(timedOutReceivers, waitDuration);
    }
    
    static final class Result {
        private Set<OutgoingMessageResponseListener<InetSocketAddress>> timedOutReceivers;
        private long waitDuration;

        public Result(Set<OutgoingMessageResponseListener<InetSocketAddress>> timedOutReceivers, long waitDuration) {
            this.timedOutReceivers = Collections.unmodifiableSet(timedOutReceivers);
            this.waitDuration = waitDuration;
        }

        public Set<OutgoingMessageResponseListener<InetSocketAddress>> getTimedOutReceivers() {
            return timedOutReceivers;
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
