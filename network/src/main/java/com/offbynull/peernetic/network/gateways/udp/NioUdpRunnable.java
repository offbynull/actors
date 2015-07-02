package com.offbynull.peernetic.network.gateways.udp;

import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class NioUdpRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(NioUdpRunnable.class);
    
    private final InetSocketAddress bindAddress;
    private final int bufferSize;

    private final Address selfPrefix;
    private final Address toPrefix;
    private final Shuttle outShuttle; // output shuttle where messages are supposed to go
    private final Bus bus; // bus from this gateway's shuttle
    private final Serializer serializer;
    
    private final LinkedBlockingQueue<IncomingPacket> incomingPacketQueue;
    private final LinkedBlockingQueue<OutgoingPacket> outgoingPacketQueue;

    public NioUdpRunnable(Address selfPrefix, Address toPrefix, Shuttle outShuttle, Bus bus, Serializer serializer,
            InetSocketAddress bindAddress, int bufferSize) {
        Validate.notNull(selfPrefix);
        Validate.notNull(toPrefix);
        Validate.notNull(outShuttle);
        Validate.notNull(bus);
        Validate.notNull(serializer);
        Validate.notNull(bindAddress);
        Validate.isTrue(bufferSize > 0);
        
        this.selfPrefix = selfPrefix;
        this.toPrefix = toPrefix;
        this.outShuttle = outShuttle;
        this.bus = bus;
        this.serializer = serializer;
        this.bindAddress = bindAddress;
        this.bufferSize = bufferSize;
        
        this.incomingPacketQueue = new LinkedBlockingQueue<>();
        this.outgoingPacketQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {
        LOG.info("NIO UDP runnable started");
        
        
        LOG.info("Setting up channel and selector");
        Selector selector = null;
        DatagramChannel channel = null;
        SelectionKey selectionKey = null;
        try {
            selector = Selector.open();
            channel = DatagramChannel.open();
            channel.socket().bind(bindAddress);
            channel.configureBlocking(false);
            selectionKey = channel.register(selector, SelectionKey.OP_READ);
        } catch (RuntimeException | IOException e) {
            LOG.error("Error setting up channel and/or selector", e);
            IOUtils.closeQuietly(selector);
            IOUtils.closeQuietly(channel);
            return;
        }
        
        
        LOG.info("Creating message pump threads");
        // incoming messages that get funneled from the datagramchannel out to some destination shuttle
        LinkedList<IncomingPacket> incomingPackets = new LinkedList<>();
        Runnable inRunnable = new IncomingPumpRunnable(selfPrefix, toPrefix, serializer, incomingPacketQueue, outShuttle);
        Thread inThread = new Thread(inRunnable);
        // outgoing messages that go from our shuttle to the datagramchannel
        LinkedList<OutgoingPacket> outgoingPackets = new LinkedList<>();
        Runnable outRunnable = new OutgoingPumpRunnable(selfPrefix, serializer, bus, outgoingPacketQueue, selector);
        Thread outThread = new Thread(outRunnable);

        
        try {
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            
            LOG.info("Starting message pump threads");
            inThread.start();
            outThread.start();
            
            
            LOG.info("Starting selector loop");
            int lastOps = 0;
            while (true) {
                incomingPackets.clear();

                // Calculate interests -- always register read, and if we have pending packets to send, register write.
                int ops = SelectionKey.OP_READ;
                if (!outgoingPackets.isEmpty()) {
                    ops |= SelectionKey.OP_WRITE;
                }

                // Register ops -- some locking probably happens in interestOps(), so check to make sure value is different before registering
                if (ops != lastOps) {
                    selectionKey.interestOps(ops);
                }

                // Select
                selector.select();
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();

                while (selectedKeys.hasNext()) {

                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isReadable()) {
                        buffer.clear();

                        // Read
                        InetSocketAddress addr = (InetSocketAddress) channel.receive(buffer);
                        buffer.flip();

                        // Copy
                        byte[] bufferCopy = new byte[buffer.limit()];
                        buffer.get(bufferCopy);

                        // Insert incoming packet
                        IncomingPacket incomingPacket = new IncomingPacket(bufferCopy, addr);
                        incomingPackets.add(incomingPacket);
                    } else if (key.isWritable()) {
                        buffer.clear();

                        // Get outgoing packet
                        OutgoingPacket outgoingPacket = outgoingPackets.removeFirst();

                        if (outgoingPacket == null) {
                            // nothing to write -- probably should never happen, but maybe can happen? either way, if happens just skip
                            //LOG.warn("OP_WRITE triggered but no outgoing packet available");
                            continue;
                        }

                        // Copy
                        buffer.put(outgoingPacket.getPacket());
                        buffer.flip();

                        // Send
                        InetSocketAddress addr = outgoingPacket.getDestinationSocketAddress();
                        channel.send(buffer, addr);
                    }
                }

                // Add packets that came in to in-queue
                incomingPacketQueue.addAll(incomingPackets);
                // Drain outgoing packets to out-queue
                outgoingPacketQueue.drainTo(outgoingPackets);
            }
        } catch (IOException e) {
            LOG.error("Serious error has occurred, shutting down thread", e);
        } catch (ClosedSelectorException e) {
            LOG.info("Selector has been closed");
        } finally {
            LOG.info("Shutting down selector and channel");
            IOUtils.closeQuietly(selector);
            IOUtils.closeQuietly(channel);
            
            
            LOG.info("Shutting down input message pump thread");
            inThread.interrupt();
            try {
                inThread.join();
            } catch (InterruptedException ie) {
                Thread.interrupted();
                LOG.warn("Interrupted while waiting for input message pump shutdown");
            }
            
            
            LOG.info("Shutting down output message pump thread");
            outThread.interrupt();
            try {
                outThread.join();
            } catch (InterruptedException ie) {
                Thread.interrupted();
                LOG.warn("Interrupted while waiting for output message pump shutdown");
            }
        }
    }
}
