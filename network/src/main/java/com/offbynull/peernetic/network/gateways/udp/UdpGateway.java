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
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.peernetic.core.shuttles.simple.SimpleShuttle;
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

    private final SimpleShuttle srcShuttle;
    private final NioUdpRunnable nioUdpRunnable;
    private final Thread nioUdpThread;
    
    
    /**
     * Constructs a {@link UdpGateway} instance.
     * @param bindAddress internet address to bind UDP socket to
     * @param prefix address prefix for this gateway
     * @param proxyShuttle shuttle to forward incoming messages to / shuttle to accept outgoing messagse from
     * @param proxyAddress address to forward incoming messages to / address to accept outgoing messages from
     * @param serializer serializer to use for serialization / deserialization
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code proxyShuttle}'s address prefix is not a prefix of {@code proxyAddress}
     * @throws IllegalStateException if failed to set up UDP channel
     */
    public UdpGateway(
            InetSocketAddress bindAddress,
            String prefix,
            Shuttle proxyShuttle,
            Address proxyAddress,
            Serializer serializer) {
        Validate.notNull(bindAddress);
        Validate.notNull(prefix);
        Validate.notNull(proxyShuttle);
        Validate.notNull(proxyAddress);
        Validate.notNull(serializer);
        
        // Validate outgoingAddress is for outgoignShuttle
        Address outgoingPrefix = Address.of(proxyShuttle.getPrefix());
        Validate.isTrue(outgoingPrefix.isPrefixOf(proxyAddress));
        
        Bus bus = new Bus();
        srcShuttle = new SimpleShuttle(prefix, bus);
        Address selfPrefix = Address.of(prefix);
        
        nioUdpRunnable = new NioUdpRunnable(selfPrefix, proxyAddress, proxyShuttle, bus, serializer, bindAddress, 65535);
        nioUdpThread = new Thread(nioUdpRunnable, "NIO UDP - " + bindAddress);
        nioUdpThread.setDaemon(true);
        nioUdpThread.start();
    }
    
    @Override
    public Shuttle getIncomingShuttle() {
        return srcShuttle;
    }

    @Override
    public void close() throws Exception {
        nioUdpRunnable.close();
        nioUdpThread.join();
    }
}
