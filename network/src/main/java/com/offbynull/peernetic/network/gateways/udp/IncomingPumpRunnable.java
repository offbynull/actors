package com.offbynull.peernetic.network.gateways.udp;

import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
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
    private final Address recverPrefix;
    private final Serializer serializer;
    
    // from udp NIO thread to this pump
    private final Bus inBus; // this bus is not linked to any shuttle... it's only being used instead of a linkedblockingqueue because
                             // we have the ability to close it and not cause a memory leak if we encounter an error in this thread
    
    // from this pump to a Shuttle
    private final Shuttle outShuttle;
    
    public IncomingPumpRunnable(String selfPrefix, Address recverPrefix, Serializer serializer, Bus inQueue, Shuttle outShuttle) {
        Validate.notNull(selfPrefix);
        Validate.notNull(recverPrefix);
        Validate.notNull(serializer);
        Validate.notNull(inQueue);
        Validate.notNull(outShuttle);
        this.selfPrefix = Address.of(selfPrefix);
        this.recverPrefix = recverPrefix;
        this.serializer = serializer;
        this.inBus = inQueue;
        this.outShuttle = outShuttle;
    }
    
    @Override
    public void run() {
        try {
            
            while (true) {
                // Poll for new packets
                List<Object> incomingPackets = inBus.pull();
                
                Validate.notNull(incomingPackets);
                Validate.noNullElements(incomingPackets);

                // Process messages
                List<Message> outgoingMessages = new ArrayList<>(incomingPackets.size());
                
                for (Object incomingPacketObj : incomingPackets) {
                    try {
                        IncomingPacket incomingPacket = (IncomingPacket) incomingPacketObj;
                        
                        byte[] packet = incomingPacket.getPacket();
                        InetSocketAddress srcSocketAddr = incomingPacket.getSourceSocketAddress();
                        
                        EncapsulatedMessage em = serializer.deserialize(packet);

                        Address srcAddress = selfPrefix
                                .appendSuffix(toShuttleAddress(srcSocketAddr))
                                .appendSuffix(em.getSourceSuffix());
                        Address dstAddress = recverPrefix
                                .appendSuffix(em.getDestinationSuffix());
                        Object payload = em.getObject();

                        Message msg = new Message(srcAddress, dstAddress, payload);
                        outgoingMessages.add(new Message(srcAddress, dstAddress, msg));
                        
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
        } finally {
            inBus.close();
        }
    }
    
    private static String toShuttleAddress(SocketAddress address) {
        byte[] addr = ((InetSocketAddress) address).getAddress().getAddress();
        int port = ((InetSocketAddress) address).getPort();
        
        return Hex.encodeHexString(addr) + '.' + port;
    }
}
