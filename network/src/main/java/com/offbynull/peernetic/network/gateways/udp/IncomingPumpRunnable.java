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

import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// What's the point of this thread? The point is that we don't want to deserialize and push messages to the shuttle from the NIO thread
// that's reading from / writing to the UDP socket. Deserializers and shuttles should always be fast and never block, but it depends on
// the implementations if this is the case. As such, we don't want to run in to a situation where the NIO thread is slowing down because
// the deserializer is slow or pushing out a message to a shuttle is slow.
//
// This thread is basically an intermediary between NIO and the receiving shuttle. If deserialization is slow or the shuttle is slow, it
// won't effect NIO selection/reading/writing. Incoming messages will just get queued up until they're reading to be deserialized and
// sent to the shuttle.
final class IncomingPumpRunnable implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(IncomingPumpRunnable.class);

    private final Address selfPrefix;
    private final Address proxyPrefix;
    private final Serializer serializer;
    
    // from udp NIO thread to this pump
    private final LinkedBlockingQueue<IncomingPacket> inQueue;
    private final Shuttle outShuttle;
    
    public IncomingPumpRunnable(Address selfPrefix, Address proxyPrefix, Shuttle proxyShuttle, Serializer serializer,
            LinkedBlockingQueue<IncomingPacket> inQueue) {
        Validate.notNull(selfPrefix);
        Validate.notNull(proxyPrefix);
        Validate.notNull(proxyShuttle);
        Validate.notNull(serializer);
        Validate.notNull(inQueue);
        this.selfPrefix = selfPrefix;
        this.proxyPrefix = proxyPrefix; // the address we're suppose to funnel stuff in to, should be a child of proxyShuttle's address
        this.outShuttle = proxyShuttle;
        this.serializer = serializer;
        this.inQueue = inQueue;
        Validate.isTrue(Address.of(proxyShuttle.getPrefix()).isPrefixOf(proxyPrefix));
    }
    
    @Override
    public void run() {
        try {
            
            while (true) {
                List<IncomingPacket> incomingPackets = new LinkedList<>();
                
                // Poll for new packets
                IncomingPacket first = inQueue.take();
                incomingPackets.add(first);
                inQueue.drainTo(incomingPackets);
                
                Validate.notNull(incomingPackets);
                Validate.noNullElements(incomingPackets);

                // Process messages
                List<Message> outgoingMessages = new ArrayList<>(incomingPackets.size());
                
                for (IncomingPacket incomingPacketObj : incomingPackets) {
                    try {
                        IncomingPacket incomingPacket = incomingPacketObj;
                        
                        byte[] packet = incomingPacket.getPacket();
                        InetSocketAddress srcSocketAddr = incomingPacket.getSourceSocketAddress();
                        
                        EncapsulatedMessage em = serializer.deserialize(packet);

                        Address srcAddress = selfPrefix
                                .appendSuffix(toShuttleAddress(srcSocketAddr))
                                .appendSuffix(em.getSourceSuffix());
                        Address dstAddress = proxyPrefix
                                .appendSuffix(em.getDestinationSuffix());
                        Object payload = em.getObject();

                        Message msg = new Message(srcAddress, dstAddress, payload);
                        outgoingMessages.add(msg);
                        
                        LOG.debug("Incoming packet from {}: {}", srcSocketAddr, msg);
                    } catch (Exception e) {
                        LOG.error("Error processing packet: " + incomingPacketObj, e);
                    }
                }
                
                // Send messages to shuttle
                outShuttle.send(outgoingMessages);
            }
        } catch (InterruptedException ie) {
            LOG.debug("Udp incoming message pump interrupted");
            Thread.interrupted();
        } catch (Exception e) {
            LOG.error("Internal error encountered", e);
        }
    }
    
    private static String toShuttleAddress(SocketAddress address) {
        byte[] addr = ((InetSocketAddress) address).getAddress().getAddress();
        int port = ((InetSocketAddress) address).getPort();
        
        return Hex.encodeHexString(addr) + '.' + port;
    }
}
