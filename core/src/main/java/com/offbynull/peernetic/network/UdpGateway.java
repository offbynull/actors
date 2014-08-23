package com.offbynull.peernetic.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

public final class UdpGateway implements Gateway<InetSocketAddress> {

//    private final InetSocketAddress bindAddress;
    private final Channel channel;
    private final EventLoopGroup eventLoopGroup;
    private final boolean closeEventLoopGroup;

    public UdpGateway(int port, GatewayListener listener, Serializer serializer) {
        this(new InetSocketAddress(port), null, listener, serializer);
    }

    public UdpGateway(InetSocketAddress bindAddress, GatewayListener<InetSocketAddress> listener, Serializer serializer) {
        this(bindAddress, null, listener, serializer);
    }
    
    public UdpGateway(InetSocketAddress bindAddress, EventLoopGroup eventLoopGroup, GatewayListener<InetSocketAddress> listener,
            Serializer serializer) {
        Validate.notNull(bindAddress);
//        Validate.notNull(eventLoopGroup); // can be null
        Validate.notNull(listener);
        Validate.notNull(serializer);
        

//        this.bindAddress = bindAddress;

        if (eventLoopGroup != null) {
            this.eventLoopGroup = eventLoopGroup;
            this.closeEventLoopGroup = false;
        } else {
            this.eventLoopGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(NioEventLoopGroup.class, true));
            this.closeEventLoopGroup = true;
        }

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
                                    .addLast(new ReadToListenerHandler(UdpGateway.this, listener));
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
    }

    @Override
    public void send(InetSocketAddress destination, Object message) {
        DefaultAddressedEnvelope datagramPacket = new DefaultAddressedEnvelope(message, destination);
        channel.writeAndFlush(datagramPacket);
    }

    @Override
    public void close() throws Exception {
        if (closeEventLoopGroup) {
            this.eventLoopGroup.shutdownGracefully();
        }
        channel.close().sync();
    }
}
