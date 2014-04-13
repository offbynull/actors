package com.offbynull.peernetic.nettyextensions.builders;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import java.io.Closeable;

public final class NettyClient implements Closeable {
    private Channel channel;
    private EventLoopGroup eventLoopGroup;

    public NettyClient(Channel channel, EventLoopGroup eventLoopGroup) {
        this.channel = channel;
        this.eventLoopGroup = eventLoopGroup;
    }
    
    public Channel channel() {
        return channel;
    }
    
    public EventLoopGroup eventLoop() {
        return eventLoopGroup;
    }

    @Override
    public void close() {
        try {
            channel.close().sync();
        } catch (Exception ex) {
            // do nothing
        }
        try {
            eventLoopGroup.shutdownGracefully().get();
        } catch (Exception ex) {
            // do nothing
        }
    }
    
}
