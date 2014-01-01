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
package com.offbynull.peernetic.rpc.transport.transports.udp;

import com.offbynull.peernetic.common.concurrent.actor.Message.ResponseDetails;
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
    
    public void addRequestId(InetSocketAddress dest, MessageId id, ResponseDetails responseDetails,
            long currentTime) {
        Validate.notNull(dest);
        Validate.notNull(id);
        Validate.notNull(responseDetails);
        
        MessageIdInstance idInstance = new MessageIdInstance(dest, id, PacketType.REQUEST);
        
        Validate.isTrue(!messageIdSet.containsKey(idInstance));
        
        Entity entity = new Entity(currentTime + timeout, idInstance, responseDetails);
        
        messageIdSet.put(idInstance, entity);
        idQueue.addLast(entity);
    }

    public ResponseDetails getResponseDetails(InetSocketAddress dest, MessageId id) {
        Validate.notNull(dest);
        Validate.notNull(id);
        
        MessageIdInstance idInstance = new MessageIdInstance(dest, id, PacketType.REQUEST);
        
        Entity entity = messageIdSet.get(idInstance);
        
        return entity == null ? null : entity.getResponseDetails();
    }
    
    public Result pending() {
        Set<ResponseDetails> timedOutReceivers = new HashSet<>();
        
        for (Entity entity : idQueue) {
            timedOutReceivers.add(entity.getResponseDetails());
        }
        
        return new Result(timedOutReceivers, 0L);
    }
    
    public Result evaluate(long currentTime) {
        Set<ResponseDetails> timedOut = new HashSet<>();
        long waitDuration = 0L;
        
        while (true) {
            Entity entity = idQueue.peekFirst();
            
            if (entity == null) {
                break;
            }
            
            if (currentTime >= entity.getTimeoutTimestamp()) {
                timedOut.add(entity.getResponseDetails());
                idQueue.pollFirst();
            } else {
                waitDuration = entity.getTimeoutTimestamp() - currentTime;
                if (waitDuration <= 0L) {
                    waitDuration = 1L;
                }
                break;
            }
        }
        
        return new Result(timedOut, waitDuration);
    }
    
    static final class Result {
        private Set<ResponseDetails> timedOut;
        private long waitDuration;

        public Result(Set<ResponseDetails> timedOut, long waitDuration) {
            this.timedOut = Collections.unmodifiableSet(timedOut);
            this.waitDuration = waitDuration;
        }

        public Set<ResponseDetails> getTimedOut() {
            return timedOut;
        }

        public long getWaitDuration() {
            return waitDuration;
        }
        
    }
    
    private static final class Entity {
        private long timeoutTimestamp;
        private ResponseDetails responseDetails;
        private MessageIdInstance instance;

        public Entity(long timeoutTimestamp, MessageIdInstance instance, ResponseDetails responseDetails) {
            this.timeoutTimestamp = timeoutTimestamp;
            this.instance = instance;
            this.responseDetails = responseDetails;
        }

        public long getTimeoutTimestamp() {
            return timeoutTimestamp;
        }

        public MessageIdInstance getInstance() {
            return instance;
        }

        public ResponseDetails getResponseDetails() {
            return responseDetails;
        }
        
    }
}
