package com.offbynull.peernetic.network.gateways.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;

final class NioUdpRunnable implements Runnable {

    private final Selector selector;
    private final SelectionKey selectionKey;
    private final DatagramChannel channel;
    private final int bufferSize;

    private final LinkedBlockingQueue<IncomingPacket> incomingPacketQueue;
    private final LinkedBlockingQueue<OutgoingPacket> outgoingPacketQueue;

    public NioUdpRunnable(Selector selector, SelectionKey selectionKey, DatagramChannel channel, int bufferSize,
            LinkedBlockingQueue<IncomingPacket> incomingPacketQueue, LinkedBlockingQueue<OutgoingPacket> outgoingPacketQueue) {
        Validate.notNull(selector);
        Validate.notNull(selectionKey);
        Validate.notNull(channel);
        Validate.isTrue(bufferSize > 0);
        Validate.notNull(incomingPacketQueue);
        this.selector = selector;
        this.selectionKey = selectionKey;
        this.channel = channel;
        this.bufferSize = bufferSize;
        this.incomingPacketQueue = incomingPacketQueue;
        this.outgoingPacketQueue = outgoingPacketQueue;
    }

    @Override
    public void run() {
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

        LinkedList<IncomingPacket> incomingPackets = new LinkedList<>();
        LinkedList<OutgoingPacket> outgoingPackets = new LinkedList<>();

        while (true) {
            incomingPackets.clear();

            // Register interests -- always read, and if we have pending packets to send, register write.
            int ops = SelectionKey.OP_READ;
            if (!outgoingPackets.isEmpty()) {
                ops |= SelectionKey.OP_WRITE;
            }
            selectionKey.interestOps(ops);

            // Select
            selector.select();
            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();

            while (selectedKeys.hasNext()) {
                try {
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

                        // Insert
                        IncomingPacket incomingPacket = new IncomingPacket(bufferCopy, addr);
                        incomingPackets.add(incomingPacket);
                    } else if (key.isWritable()) {
                        buffer.clear();

                        // Get next packet
                        OutgoingPacket outgoingPacket = outgoingPackets.removeFirst();

                        // Copy
                        buffer.put(outgoingPacket.getPacket());
                        buffer.flip();

                        // Send
                        InetSocketAddress addr = outgoingPacket.getDestinationSocketAddress();
                        channel.send(buffer, addr);
                    }
                } catch (IOException e) {
                    System.err.println("glitch, continuing... " + (e.getMessage() != null ? e.getMessage() : ""));
                }
            }

            // Add packets that came in to in-queue
            incomingPacketQueue.addAll(incomingPackets);
            outgoingPacketQueue.drainTo(outgoingPackets);
        }
    }
}
