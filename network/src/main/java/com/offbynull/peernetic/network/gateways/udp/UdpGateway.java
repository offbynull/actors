/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.network.gateways.udp;

import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.gateway.InputGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

public final class UdpGateway implements InputGateway {

    private final String prefix;
    private final Shuttle srcShuttle;
    private final Shuttle dstShuttle;
    private final Address dstId;
    
    private final Channel channel;
    private final EventLoopGroup eventLoopGroup;
    private final boolean closeEventLoopGroup;
    
    
    public UdpGateway(InetSocketAddress bindAddress, String prefix, Shuttle outgoingShuttle, Address dstId, Serializer serializer) {
        Validate.notNull(bindAddress);
        Validate.notNull(prefix);
        Validate.notNull(outgoingShuttle);
        Validate.notNull(dstId);
        Validate.notNull(serializer);
        
        this.prefix = prefix;
        this.dstShuttle = outgoingShuttle;
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
                                    .addLast(new IncomingMessageShuttleHandler(Address.of(prefix), outgoingShuttle, dstId));
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
    public Shuttle getIncomingShuttle() {
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
