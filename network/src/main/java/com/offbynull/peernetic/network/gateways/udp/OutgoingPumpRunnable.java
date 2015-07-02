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
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// What's the point of this thread? The point is that we never want a Shuttle implementation that directly wakes up the selector, because we
// don't know how the selector performs (its dependent on platform/drivers). We never want a Shuttle to block.
//
// This thread is basically an intermediary between the Shuttle and NIO. If wakeup ever slows down or blocks it won't effect the shuttle
// pushing messages to this intermediary. Additional messages can still be added to the shuttle, it'll just be queued until this thread gets
// a chance to read.
final class OutgoingPumpRunnable implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(OutgoingPumpRunnable.class);

    private final Address selfPrefix;
    private final Address proxyPrefix;
    private final Serializer serializer;
    
    // from this gateway's Shuttle to this pump
    private final Bus bus;
    
    // from this pump to the udp NIO thread
    private final LinkedBlockingQueue<OutgoingPacket> outQueue;
    private final Selector outSelector; // selector is what blocks in the NIO thread

    public OutgoingPumpRunnable(Address selfPrefix, Address proxyPrefix, Serializer serializer, Bus bus,
            LinkedBlockingQueue<OutgoingPacket> outQueue, Selector outSelector) {
        Validate.notNull(selfPrefix);
        Validate.notNull(proxyPrefix);
        Validate.notNull(serializer);
        Validate.notNull(bus);
        Validate.notNull(outQueue);
        Validate.notNull(outSelector);
        this.selfPrefix = selfPrefix;
        this.proxyPrefix = proxyPrefix;
        this.serializer = serializer;
        this.bus = bus;
        this.outQueue = outQueue;
        this.outSelector = outSelector;
    }
    
    @Override
    public void run() {
        try {
            
            while (true) {
                // Poll for new messages from this gateway's shuttle
                List<Object> incomingObjects = bus.pull();
                Validate.notNull(incomingObjects);
                Validate.noNullElements(incomingObjects);

                List<OutgoingPacket> outgoingPackets = new ArrayList<>(incomingObjects.size());
                
                for (Object incomingObj : incomingObjects) {
                    if (incomingObj instanceof Message) {
                        Message msg = (Message) incomingObj;
                        
                        try {
                            Address src = msg.getSourceAddress();
                            Address dst = msg.getDestinationAddress();
                            Object payload = msg.getMessage();

                            Validate.isTrue(selfPrefix.isPrefixOf(dst));
                            Validate.isTrue(dst.size() >= 2);
                            String dstPrefix = dst.getElement(0);
                            String dstAddress = dst.getElement(1);
                            InetSocketAddress dstAddr = fromShuttleAddress(dstAddress);
                            Address dstSuffix = dst.removePrefix(Address.of(dstPrefix, dstAddress));

                            Validate.isTrue(proxyPrefix.isPrefixOf(src));
                            Address srcSuffix = src.removePrefix(proxyPrefix);

                            EncapsulatedMessage em = new EncapsulatedMessage(srcSuffix, dstSuffix, payload);
                            byte[] serializedEm = serializer.serialize(em);
                            
                            OutgoingPacket outgoingPacket = new OutgoingPacket(serializedEm, dstAddr);
                            outgoingPackets.add(outgoingPacket);

                            LOG.debug("Outgoing packet to {}: {}", dstAddr, msg.getMessage());
                        } catch (Exception e) {
                            LOG.error("Error processing message: " + msg, e);
                        }
                    } else {
                        throw new IllegalStateException("Unexpected message type: " + incomingObj);
                    }
                }
                
                
                if (!outgoingPackets.isEmpty()) {
                    // Notify selector
                    outQueue.addAll(outgoingPackets);
                    outSelector.wakeup();
                }
            }
        } catch (InterruptedException ie) {
            LOG.debug("Udp outgoing message pump interrupted");
            Thread.interrupted();
        } catch (Exception e) {
            LOG.error("Internal error encountered", e);
        } finally {
            bus.close();
        }
    }
    
    private static InetSocketAddress fromShuttleAddress(String address) throws UnknownHostException, DecoderException {
        int splitIdx = address.indexOf('.');
        Validate.isTrue(splitIdx != -1);

        String addrStr = address.substring(0, splitIdx);
        String portStr = address.substring(splitIdx + 1);

        InetAddress addr = InetAddress.getByAddress(Hex.decodeHex(addrStr.toCharArray()));
        int port = Integer.parseInt(portStr);

        return new InetSocketAddress(addr, port);
    }
}
