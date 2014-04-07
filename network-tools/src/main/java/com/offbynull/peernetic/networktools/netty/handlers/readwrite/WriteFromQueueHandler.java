package com.offbynull.peernetic.networktools.netty.handlers.readwrite;

import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.util.concurrent.ScheduledFuture;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public final class WriteFromQueueHandler extends ChannelHandlerAdapter {
    private BlockingQueue<Message> queue;
    private long pollRate;
    private boolean requiresEnvelope;
    private volatile ScheduledFuture<?> scheduledFuture;

    public WriteFromQueueHandler(BlockingQueue<Message> queue, long pollInterval, boolean requiresEnvelope) {
        this.queue = queue;
        this.pollRate = pollInterval;
        this.requiresEnvelope = requiresEnvelope;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        scheduledFuture = ctx.channel().eventLoop().scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                List<Message> out = new ArrayList<>();
                queue.drainTo(out);
                
                for (Message obj : out) {
                    if (requiresEnvelope) {
                        AddressedEnvelope<Object, SocketAddress> env =
                                new DefaultAddressedEnvelope<>(obj.getMessage(), obj.getRemoteAddress(), obj.getLocalAddress());
                        ctx.channel().write(env);
                    } else {
                        ctx.channel().write(obj.getMessage());
                    }
                }
                
                ctx.flush();
            }
        }, pollRate, pollRate, TimeUnit.MILLISECONDS);
        
        super.channelActive(ctx); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }
    
}
