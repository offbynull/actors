package com.offbynull.p2prpc.transport;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.IOUtils;

public final class UdpTransport implements NonSessionedTransport<InetSocketAddress> {
    private InetSocketAddress listenAddress;
    private EventLoop eventLoop;
    private int bufferSize;

    public UdpTransport(int port) {
        this(new InetSocketAddress(port), 65535);
    }

    public UdpTransport(int port, int bufferSize) {
        this(new InetSocketAddress(port), bufferSize);
    }

    public UdpTransport(InetSocketAddress listenAddress, int bufferSize) {
        this.listenAddress = listenAddress;
        this.bufferSize = bufferSize;
    }
    
    @Override
    public void start() throws IOException {
        if (eventLoop != null) {
            throw new IllegalStateException();
        }

        eventLoop = new EventLoop(bufferSize, listenAddress);
        eventLoop.startAndWait();
    }

    @Override
    public void stop() throws IOException {
        if (eventLoop == null || !eventLoop.isRunning()) {
            throw new IllegalStateException();
        }

        eventLoop.stopAndWait();
    }

    @Override
    public ReceiveNotifier getReceiveNotifier() {
        if (eventLoop == null || !eventLoop.isRunning()) {
            throw new IllegalStateException();
        }
        
        return eventLoop.getReceiveNotifier();
    }

    @Override
    public PacketSender getPacketSender() {
        if (eventLoop == null || !eventLoop.isRunning()) {
            throw new IllegalStateException();
        }
        
        return eventLoop.getPacketSender();
    }

    private final class EventLoop extends AbstractExecutionThreadService {

        private int bufferSize;
        private InetSocketAddress listenAddress;

        private Selector selector;
        private DatagramChannel channel;
        private AtomicBoolean stop;
        
        private UdpReceiveNotifier receiveNotifier;
        private UdpPacketSender packetSender;

        public EventLoop(int bufferSize, InetSocketAddress listenAddress) throws IOException {
            this.bufferSize = bufferSize;
            this.listenAddress = listenAddress;
            
            try {
                selector = Selector.open();
                channel = DatagramChannel.open();
            } catch (RuntimeException | IOException e) {
                IOUtils.closeQuietly(selector);
                IOUtils.closeQuietly(channel);
                throw e;
            }
            
            receiveNotifier = new UdpReceiveNotifier();
            packetSender = new UdpPacketSender(selector);
        }
        
        @Override
        protected void startUp() throws IOException {
            Thread.currentThread().setDaemon(false);
            
            stop = new AtomicBoolean(false);
            try {
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_READ);
                channel.socket().bind(listenAddress);
            } catch (RuntimeException | IOException e) {
                IOUtils.closeQuietly(selector);
                IOUtils.closeQuietly(channel);
                throw e;
            }
        }

        public UdpReceiveNotifier getReceiveNotifier() {
            return receiveNotifier;
        }

        public UdpPacketSender getPacketSender() {
            return packetSender;
        }

        @Override
        protected void run() throws Exception {
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

            int selectionKey = SelectionKey.OP_READ;
            LinkedList<OutgoingPacket<InetSocketAddress>> pendingOutgoingPackets = new LinkedList<>();
            LinkedList<IncomingPacket<InetSocketAddress>> pendingIncomingPackets = new LinkedList<>();
            while (true) {
                // get outgoing data
                packetSender.drainTo(pendingOutgoingPackets);
                
                // set selection key based on if there's outgoing data available
                int newSelectionKey = SelectionKey.OP_READ;
                if (!pendingOutgoingPackets.isEmpty()) {
                    newSelectionKey |= SelectionKey.OP_WRITE;
                }
                
                if (newSelectionKey != selectionKey) {
                    selectionKey = newSelectionKey;
                    channel.register(selector, selectionKey);
                }
                
                // select
                selector.select();

                // stop if signalled
                if (stop.get()) {
                    return;
                }
                
                // get current time
                long currentTime = System.currentTimeMillis();
                
                // go through selected keys
                Iterator keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = (SelectionKey) keys.next();
                    keys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isReadable()) { // incoming data available
                        InetSocketAddress from = (InetSocketAddress) channel.receive(buffer);

                        buffer.flip();
                        byte[] inData = new byte[buffer.remaining()];
                        buffer.get(inData);
                        buffer.clear();
                        
                        IncomingPacket<InetSocketAddress> packet = new IncomingPacket<>(from, inData, currentTime);
                        pendingIncomingPackets.addLast(packet);
                    } else if (key.isWritable()) { // ready for outgoing data
                        OutgoingPacket<InetSocketAddress> packet = pendingOutgoingPackets.poll();
                        
                        if (packet != null) {
                            channel.send(packet.getData(), packet.getTo());
                        }
                    }
                }
                
                receiveNotifier.notify(pendingIncomingPackets);
                pendingIncomingPackets.clear();
            }
        }

        @Override
        protected void shutDown() throws Exception {
            IOUtils.closeQuietly(selector);
            IOUtils.closeQuietly(channel);
        }

        @Override
        protected String serviceName() {
            return UdpTransport.class.getSimpleName() + " Event Loop (" + listenAddress.toString() + ")";
        }

        @Override
        protected void triggerShutdown() {
            stop.set(true);
            selector.wakeup();
        }
    }
    
    public static final class UdpReceiveNotifier implements ReceiveNotifier<InetSocketAddress> {
        private LinkedBlockingQueue<PacketReceiver> handlers;
        
        private UdpReceiveNotifier() {
            handlers = new LinkedBlockingQueue<>();
        }

        @Override
        public void add(PacketReceiver<InetSocketAddress> e) {
            handlers.add(e);
        }

        @Override
        public void remove(PacketReceiver<InetSocketAddress> e) {
            handlers.remove(e);
        }

        private void notify(Collection<IncomingPacket<InetSocketAddress>> packets) {
            PacketReceiver[] handlersArray = handlers.toArray(new PacketReceiver[0]);
            
            for (IncomingPacket<InetSocketAddress> packet : packets) {
                for (PacketReceiver<InetSocketAddress> handler : handlersArray) { // to array to avoid locks
                    if (handler.packetArrived(packet)) {
                        break;
                    }
                }
            }
        }
    }
    
    public static final class UdpPacketSender implements PacketSender<InetSocketAddress> {
        private Selector selector;
        private LinkedBlockingQueue<OutgoingPacket<InetSocketAddress>> outgoingPackets;

        private UdpPacketSender(Selector selector) {
            this.selector = selector;
            this.outgoingPackets = new LinkedBlockingQueue<>();
        }
        
        @Override
        public void sendPacket(OutgoingPacket<InetSocketAddress> packet) {
            outgoingPackets.add(packet);
            selector.wakeup();
        }
        
        private void drainTo(Collection<OutgoingPacket<InetSocketAddress>> destination) {
            outgoingPackets.drainTo(destination);
        }
    }
}
