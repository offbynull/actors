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

final class OutgoingMessagePumpRunnable implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(OutgoingMessagePumpRunnable.class);

    private final Address selfPrefix;
    private final Address senderPrefix;
    
    private final Serializer serializer;
    
    // from Shuttle to this pump
    private final Bus inBus;
    
    // from this pump to the udp NIO thread
    private final LinkedBlockingQueue<Object> outQueue;
    private final Selector outSelector; // selector is what blocks in the NIO thread

    public OutgoingMessagePumpRunnable(String selfPrefix, Address senderPrefix, Serializer serializer, Bus inBus,
            LinkedBlockingQueue<Object> outQueue, Selector outSelector) {
        Validate.notNull(selfPrefix);
        Validate.notNull(senderPrefix);
        Validate.notNull(serializer);
        Validate.notNull(inBus);
        Validate.notNull(outQueue);
        Validate.notNull(outSelector);
        this.selfPrefix = Address.of(selfPrefix);
        this.senderPrefix = senderPrefix;
        this.serializer = serializer;
        this.inBus = inBus;
        this.outQueue = outQueue;
        this.outSelector = outSelector;
    }
    
    @Override
    public void run() {
        try {
            
            while (true) {
                // Poll for new messages
                List<Object> incomingObjects = inBus.pull();
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

                            Validate.isTrue(senderPrefix.isPrefixOf(src));
                            Address srcSuffix = src.removePrefix(senderPrefix);

                            EncapsulatedMessage em = new EncapsulatedMessage(srcSuffix, dstSuffix, payload);
                            byte[] serializedEm = serializer.serialize(em);
                            
                            OutgoingPacket outgoingPAcket = new OutgoingPacket(serializedEm, dstAddr);
                            outgoingPackets.add(outgoingPAcket);

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
            inBus.close();
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
