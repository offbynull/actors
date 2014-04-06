package com.offbynull.peernetic.nettyhelper.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import java.io.Closeable;

public final class NettyClient implements Closeable {
    private Channel channel;
    private EventLoopGroup eventLoopGroup;

    public NettyClient(Channel channel, EventLoopGroup eventLoopGroup) {
        this.channel = channel;
        this.eventLoopGroup = eventLoopGroup;
    }

    public ChannelFuture write(Object msg) {
        return channel.write(msg);
    }

    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return channel.write(msg, promise);
    }

    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return channel.writeAndFlush(msg, promise);
    }

    public ChannelFuture writeAndFlush(Object msg) {
        return channel.writeAndFlush(msg);
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
