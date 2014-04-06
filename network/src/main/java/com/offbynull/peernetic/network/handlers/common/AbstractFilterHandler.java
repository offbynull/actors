package com.offbynull.peernetic.network.handlers.common;

import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.net.SocketAddress;

public abstract class AbstractFilterHandler extends ChannelHandlerAdapter {

    private final boolean closeChannelOnFailure;

    public AbstractFilterHandler(boolean closeChannelOnFailure) {
        this.closeChannelOnFailure = closeChannelOnFailure;
    }
    
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        SocketAddress local = null;
        if (msg instanceof AddressedEnvelope) {
            local = ((AddressedEnvelope) msg).sender();
        }
        if (local == null) {
            local = ctx.channel().localAddress();
        }
        
        
        SocketAddress remote = null;
        if (msg instanceof AddressedEnvelope) {
            remote = ((AddressedEnvelope) msg).recipient();
        }
        if (remote == null) {
            remote = ctx.channel().remoteAddress();
        }
        
        Object content = msg instanceof AddressedEnvelope ? ((AddressedEnvelope) msg).content() : msg;
        
        if (filter(local, remote, content, Trigger.WRITE)) {
            if (closeChannelOnFailure) {
                ctx.close();
            }
            return;
        }
        
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        SocketAddress local = null;
        if (msg instanceof AddressedEnvelope) {
            local = ((AddressedEnvelope) msg).recipient();
        }
        if (local == null) {
            local = ctx.channel().localAddress();
        }
        
        
        SocketAddress remote = null;
        if (msg instanceof AddressedEnvelope) {
            remote = ((AddressedEnvelope) msg).sender();
        }
        if (remote == null) {
            remote = ctx.channel().remoteAddress();
        }
        
        Object content = msg instanceof AddressedEnvelope ? ((AddressedEnvelope) msg).content() : msg;
        
        if (filter(local, remote, content, Trigger.READ)) {
            if (closeChannelOnFailure) {
                ctx.close();
            }
            return;
        }
        
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        SocketAddress remote = ctx.channel().remoteAddress();
        SocketAddress local = ctx.channel().localAddress();
        
        if (filter(local, remote, null, Trigger.CONNECTION)) {
            if (closeChannelOnFailure) {
                ctx.close();
            }
            return;
        }
        
        super.channelActive(ctx);
    }
    
    protected abstract boolean filter(SocketAddress local, SocketAddress remote, Object content, Trigger trigger) throws Exception;
    
    public enum Trigger {
        CONNECTION,
        READ,
        WRITE
    }
}
