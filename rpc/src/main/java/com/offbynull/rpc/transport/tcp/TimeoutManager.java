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
package com.offbynull.rpc.transport.tcp;

import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.apache.commons.lang3.Validate;

final class TimeoutManager {
    private long timeout;
    private HashMap<SocketChannel, Entity> channelSet;
    private LinkedList<Entity> channelQueue;

    public TimeoutManager(long timeout) {
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);
        
        this.timeout = timeout;
        channelSet = new HashMap<>();
        channelQueue = new LinkedList<>();
    }
    
    public void addChannel(SocketChannel channel, long currentTime) {
        Validate.notNull(channel);
        Validate.isTrue(!channelSet.containsKey(channel));
        
        Entity entity = new Entity(currentTime + timeout, channel);
        
        channelSet.put(channel, entity);
        channelQueue.addLast(entity);
    }
    
    public Result pending() {
        Set<SocketChannel> channels = new HashSet<>();
        
        for (Entity entity : channelQueue) {
            channels.add(entity.getChannel());
        }
        
        return new Result(channels, 0L);
    }
    
    public Result evaluate(long currentTime) {
        Set<SocketChannel> timedOutChannels = new HashSet<>();
        long waitDuration = 0L;
        
        while (true) {
            Entity entity = channelQueue.peekFirst();
            
            if (entity == null) {
                break;
            }
            
            if (currentTime >= entity.getTimeoutTimestamp()) {
                timedOutChannels.add(entity.getChannel());
                channelQueue.pollFirst();
            } else {
                waitDuration = entity.getTimeoutTimestamp() - currentTime;
                if (waitDuration <= 0L) {
                    waitDuration = 1L;
                }
                break;
            }
        }
        
        return new Result(timedOutChannels, waitDuration);
    }
    
    static final class Result {
        private Set<SocketChannel> timedOutChannels;
        private long waitDuration;

        public Result(Set<SocketChannel> timedOutChannels, long waitDuration) {
            this.timedOutChannels = Collections.unmodifiableSet(timedOutChannels);
            this.waitDuration = waitDuration;
        }

        public Collection<SocketChannel> getTimedOutChannels() {
            return timedOutChannels;
        }

        public long getWaitDuration() {
            return waitDuration;
        }
        
    }
    
    private final class Entity {
        private long timeoutTimestamp;
        private SocketChannel channel;

        public Entity(long timeoutTimestamp, SocketChannel channel) {
            this.timeoutTimestamp = timeoutTimestamp;
            this.channel = channel;
        }

        public long getTimeoutTimestamp() {
            return timeoutTimestamp;
        }

        public SocketChannel getChannel() {
            return channel;
        }
    }
}
