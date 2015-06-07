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

import com.offbynull.peernetic.core.actor.Actor;
import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.gateway.Gateway;
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

/**
 * {@link Gateway} that sends and receives messages over UDP.
 * <p>
 * In the following example, there are two {@link Actor}s: {@code sender} and {@code echoer}. {@code sender} sends a message and waits for
 * that same message to be echoed back to it, while {@code echoer} echoes back whatever is sent to it. Both of these actors are assigned
 * their own {@link UdpGateway} (both called {@code internaludp} -- note that each actor is running in its own {@link ActorThread} so there
 * is no naming conflict here), and use that gateway to communicate with each other.
 * <p>
 * Note that UDP is unreliable. Each message is sent as a single package, and packets may come out of order or not at all. The actors in
 * this example don't account for this -- messages in this example are expected to always arrive and arrive in order.
 * <pre>
 * CountDownLatch latch = new CountDownLatch(1);
 *
 * Coroutine sender = (cnt) -&gt; {
 *     Context ctx = (Context) cnt.getContext();
 *     Address dstAddr = ctx.getIncomingMessage();
 *
 *     for (int i = 0; i &lt; 10; i++) {
 *         ctx.addOutgoingMessage(dstAddr, i);
 *         cnt.suspend();
 *         Validate.isTrue(i == (int) ctx.getIncomingMessage());
 *     }
 *
 *     // You shouldn't using threading constructs in actors. This is here for demonstration purposes.
 *     latch.countDown();
 * };
 *
 * Coroutine echoer = (cnt) -&gt; {
 *     Context ctx = (Context) cnt.getContext();
 *
 *     while (true) {
 *         Address src = ctx.getSource();
 *         Object msg = ctx.getIncomingMessage();
 *         ctx.addOutgoingMessage(src, msg);
 *         cnt.suspend();
 *     }
 * };
 *
 * 
 * // Set up echoer + a UDP gateway for it to use (on port 1000)
 * ActorThread echoerThread = ActorThread.create("echoer");
 * Shuttle echoerInputShuttle = echoerThread.getIncomingShuttle();
 * UdpGateway echoerUdpGateway = new UdpGateway(   // Create the UDP gateway for echoer
 *         new InetSocketAddress(1000),                // listen on port 1000
 *         "internaludp",                              // call this gateway "internaludp"
 *         echoerInputShuttle,                         // forward all incoming UDP messages to echoer's shuttle
 *         Address.fromString("echoer:echoer"),        // forward all incoming UDP messages to address "echoer:echoer"
 *         new SimpleSerializer());                    // use SimpleSerializer to serialize/deserialize messages
 * Shuttle echoerUdpOutputShuttle = echoerUdpGateway.getIncomingShuttle();
 * echoerThread.addOutgoingShuttle(echoerUdpOutputShuttle);
 * 
 * // Set up sender + a UDP gateway for it to use (on port 2000)
 * ActorThread senderThread = ActorThread.create("sender");
 * Shuttle senderInputShuttle = senderThread.getIncomingShuttle();
 * UdpGateway senderUdpGateway = new UdpGateway(   // Create the UDP gateway for sender
 *         new InetSocketAddress(2000),                // listen on port 1000
 *         "internaludp",                              // call this gateway "internaludp"
 *         senderInputShuttle,                         // forward all incoming UDP messages to sender's shuttle
 *         Address.fromString("sender:sender"),        // forward all incoming UDP messages to address "sender:sender"
 *         new SimpleSerializer());                    // use SimpleSerializer to serialize/deserialize messages
 * Shuttle senderUdpOutputShuttle = senderUdpGateway.getIncomingShuttle();
 * senderThread.addOutgoingShuttle(senderUdpOutputShuttle);
 *
 * // Add actors to their respective threads
 * echoerThread.addCoroutineActor("echoer", echoer);
 * senderThread.addCoroutineActor("sender", sender, Address.fromString("internaludp:7f000001.1000")); // Notify sender to send initial msg
 *                                                                                                    // over UDP to localhost:1000
 *                                                                                                    // (echoer's address)
 *
 * latch.await();
 * </pre>
 * @author Kasra Faghihi
 */
public final class UdpGateway implements InputGateway {

    private final Shuttle srcShuttle;
    
    private final Channel channel;
    private final EventLoopGroup eventLoopGroup;
    private final boolean closeEventLoopGroup;
    
    
    /**
     * Constructs a {@link UdpGateway} instance.
     * @param bindAddress address to bind to
     * @param prefix address prefix for this gateway
     * @param outgoingShuttle shuttle to forward incoming messages to
     * @param outgoingAddress address to forward incoming messages to / address to accept incoming messages from
     * @param serializer serializer to use for serialization / deserialization
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code outgoingShuttle}'s address prefix is not a prefix of {@code outgoingAddress}
     * @throws IllegalStateException if failed to set up UDP channel
     */
    public UdpGateway(
            InetSocketAddress bindAddress,
            String prefix,
            Shuttle outgoingShuttle,
            Address outgoingAddress,
            Serializer serializer) {
        Validate.notNull(bindAddress);
        Validate.notNull(prefix);
        Validate.notNull(outgoingShuttle);
        Validate.notNull(outgoingAddress);
        Validate.notNull(serializer);
        Validate.isTrue(Address.of(outgoingShuttle.getPrefix()).isPrefixOf(outgoingAddress));

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
                                    .addLast(new IncomingMessageShuttleHandler(Address.of(prefix), outgoingShuttle, outgoingAddress));
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
        this.srcShuttle = new InternalShuttle(prefix, outgoingAddress, channel);
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
