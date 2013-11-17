package com.offbynull.p2prpc.transport.tcp;

import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.apache.commons.lang3.Validate;

final class RequestManager {
    private long timeout;
    private HashMap<SocketChannel, Entity> channelSet;
    private LinkedList<Entity> channelQueue;

    public RequestManager(long timeout) {
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);
        
        this.timeout = timeout;
        channelSet = new HashMap<>();
        channelQueue = new LinkedList<>();
    }
    
    public void addRequestId(SocketChannel channel, long currentTime) {
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
