package com.offbynull.peernetic.core.gateways.udp;

import com.offbynull.peernetic.core.actor.ActorUtils;
import com.offbynull.peernetic.core.Shuttle;
import com.offbynull.peernetic.core.gateway.Gateway;
import com.offbynull.peernetic.core.gateway.Serializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

public final class UdpGateway implements Gateway {

    private final String prefix;
    private final Shuttle srcShuttle;
    private final Shuttle dstShuttle;
    private final String dstId;
    
    private final Channel channel;
    private final EventLoopGroup eventLoopGroup;
    private final boolean closeEventLoopGroup;
    
    
    public UdpGateway(InetSocketAddress bindAddress, String prefix, Shuttle dstShuttle, String dstId, Serializer serializer) {
        Validate.notNull(bindAddress);
        Validate.notNull(prefix);
        Validate.notNull(dstShuttle);
        Validate.notNull(dstId);
        Validate.notNull(serializer);
        
        Validate.notEmpty(prefix);
        // Validate.notEmpty(dstId); don't do this because dstId can be empty
        
        this.prefix = prefix;
        this.dstShuttle = dstShuttle;
        this.dstId = dstId;

        this.eventLoopGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(NioEventLoopGroup.class, true));
        this.closeEventLoopGroup = true;

        Channel channel = null;
        try {
            Bootstrap cb = new Bootstrap();
            cb.group(this.eventLoopGroup)
                    .channel(NioDatagramChannel.class)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        public void initChannel(NioDatagramChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new SerializerEncodeHandler(serializer))
                                    .addLast(new SerializerDecodeHandler(serializer))
                                    .addLast(new IncomingMessageShuttleHandler(prefix, dstShuttle, dstId));
                        }
                    });
            channel = cb.bind(bindAddress).sync().channel();
        } catch (Exception e) {
            Thread.interrupted(); // incase we were interrupted
            if (closeEventLoopGroup) {
                this.eventLoopGroup.shutdownGracefully();
            }
            // This is not required, if cb.bind.sync.channel fails channel will never be set for it to be closed
//            if (channel != null) {
//                channel.close();
//            }
            throw new IllegalStateException("Failed to build Channel", e);
        }
        
        this.channel = channel;
        this.srcShuttle = new InternalShuttle(prefix, channel);
    }
    
    @Override
    public Shuttle getShuttle() {
        return srcShuttle;
    }

    @Override
    public void close() throws Exception {
        if (closeEventLoopGroup) {
            this.eventLoopGroup.shutdownGracefully();
        }
        channel.close().sync();
    }
}
