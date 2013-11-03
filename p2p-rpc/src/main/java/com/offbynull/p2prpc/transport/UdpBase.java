package com.offbynull.p2prpc.transport;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.IOUtils;

public final class UdpBase {
    private InetSocketAddress listenAddress;
    private EventLoop eventLoop;
    private int bufferSize;

    public UdpBase(int port) {
        this(new InetSocketAddress(port), 65535);
    }

    public UdpBase(int port, int bufferSize) {
        this(new InetSocketAddress(port), bufferSize);
    }

    public UdpBase(InetSocketAddress listenAddress, int bufferSize) {
        this.listenAddress = listenAddress;
        this.bufferSize = bufferSize;
    }
    
    public void start() throws IOException {
        if (eventLoop != null) {
            throw new IllegalStateException();
        }

        eventLoop = new EventLoop(bufferSize, listenAddress);
        eventLoop.startAndWait();
    }

    public void stop() throws IOException {
        if (eventLoop == null || !eventLoop.isRunning()) {
            throw new IllegalStateException();
        }

        eventLoop.stopAndWait();
    }

    public UdpReceiveNotifier getReceiveNotifier() {
        if (eventLoop == null || !eventLoop.isRunning()) {
            throw new IllegalStateException();
        }
        
        return eventLoop.getReceiveNotifier();
    }

    public UdpSendQuerier getSendQuerier() {
        if (eventLoop == null || !eventLoop.isRunning()) {
            throw new IllegalStateException();
        }
        
        return eventLoop.getSendQuerier();
    }

    private final class EventLoop extends AbstractExecutionThreadService {

        private int bufferSize;
        private InetSocketAddress listenAddress;

        private Selector selector;
        private DatagramChannel channel;
        private AtomicBoolean stop;
        
        private UdpReceiveNotifier receiveNotifier;
        private UdpSendQuerier sendQuerier;

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
            sendQuerier = new UdpSendQuerier(selector);
        }
        
        @Override
        protected void startUp() throws IOException {
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

        public UdpSendQuerier getSendQuerier() {
            return sendQuerier;
        }

        @Override
        protected void run() throws Exception {
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

            int selectionKey = SelectionKey.OP_READ;
            LinkedList<UdpOutgoingPacket> pendingOutgoingPackets = new LinkedList<>();
            LinkedList<UdpIncomingPacket> pendingIncomingPackets = new LinkedList<>();
            while (true) {
                // get outgoing data
                sendQuerier.drainTo(pendingOutgoingPackets);
                
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
                        SocketAddress from = channel.receive(buffer);

                        buffer.flip();
                        byte[] inData = new byte[buffer.remaining()];
                        buffer.get(inData);
                        buffer.clear();
                        
                        UdpIncomingPacket packet = new UdpIncomingPacket(from, inData, currentTime);
                        pendingIncomingPackets.addLast(packet);
                    } else if (key.isWritable()) { // ready for outgoing data
                        UdpOutgoingPacket packet = pendingOutgoingPackets.poll();
                        
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
            return "UDP Base Event Loop (" + listenAddress.toString() + ")";
        }

        @Override
        protected void triggerShutdown() {
            stop.set(true);
            selector.wakeup();
        }
    }
    
    public static final class UdpReceiveNotifier {
        private LinkedBlockingQueue<UdpReceiveHandler> handlers;
        
        private UdpReceiveNotifier() {
            handlers = new LinkedBlockingQueue<>();
        }

        public void add(UdpReceiveHandler e) {
            handlers.add(e);
        }

        public void remove(UdpReceiveHandler e) {
            handlers.remove(e);
        }
        
        public void notify(UdpIncomingPacket ... packets) {
            notify(Arrays.asList(packets));
        }

        public void notify(Collection<UdpIncomingPacket> packets) {
            UdpReceiveHandler[] handlersArray = handlers.toArray(new UdpReceiveHandler[0]);
            
            for (UdpIncomingPacket packet : packets) {
                for (UdpReceiveHandler handler : handlersArray) { // to array to avoid locks
                    if (handler.incoming(packet)) {
                        break;
                    }
                }
            }
        }
    }
    
    public static final class UdpSendQuerier {
        private Selector selector;
        private LinkedBlockingQueue<UdpOutgoingPacket> outgoingPackets;

        private UdpSendQuerier(Selector selector) {
            this.selector = selector;
            this.outgoingPackets = new LinkedBlockingQueue<>();
        }
        
        public void send(SocketAddress to, byte[] data) {
            UdpOutgoingPacket packet = new UdpOutgoingPacket(to, data);
            outgoingPackets.add(packet);
            selector.wakeup();
        }
        
        public void drainTo(Collection<UdpOutgoingPacket> destination) {
            outgoingPackets.drainTo(destination);
        }
    }
    
    public interface UdpReceiveHandler {
        boolean incoming(UdpIncomingPacket packet);
    }
    
    public static final class UdpIncomingPacket {
        private SocketAddress from;
        private ByteBuffer data;
        private long arriveTime;

        public UdpIncomingPacket(SocketAddress from, byte[] data, long arriveTime) {
            this.from = from;
            this.data = ByteBuffer.allocate(data.length).put(data);
            this.arriveTime = arriveTime;
            
            this.data.flip();
        }

        public SocketAddress getFrom() {
            return from;
        }

        public ByteBuffer getData() {
            return data.asReadOnlyBuffer();
        }

        public long getArriveTime() {
            return arriveTime;
        }
        
    }
    
    public static final class UdpOutgoingPacket {
        private SocketAddress to;
        private ByteBuffer data;

        public UdpOutgoingPacket(SocketAddress to, byte[] data) {
            this.to = to;
            this.data = ByteBuffer.allocate(data.length).put(data);
            
            this.data.flip();
        }

        public SocketAddress getTo() {
            return to;
        }

        public ByteBuffer getData() {
            return data.asReadOnlyBuffer();
        }
    }
    
    public static void main(String[] args) throws Throwable {
        ServerMessageCallback<SocketAddress> callback = new ServerMessageCallback<SocketAddress>() {
            @Override
            public void messageArrived(SocketAddress from, byte[] data,
                    ServerResponseCallback responseCallback) {
                responseCallback.responseReady("OUTPUT".getBytes());
            }
        };

        UdpBase base = new UdpBase(12345);
        base.start();
        
        UdpReceiveNotifier recvNotifier = base.getReceiveNotifier();
        UdpSendQuerier sendQueuer = base.getSendQuerier();
        
        recvNotifier.add(new UdpReceiveHandler() {

            @Override
            public boolean incoming(UdpIncomingPacket packet) {
                byte[] data = new byte[packet.getData().limit()];
                packet.getData().get(data);
                System.out.println(new String(data));
                return true;
            }
        });
        
        recvNotifier.add(new UdpReceiveHandler() {

            @Override
            public boolean incoming(UdpIncomingPacket packet) {
                throw new RuntimeException();
            }
        });

        sendQueuer.send(new InetSocketAddress("localhost", 12345),
                "GET /\r\n\r\n".getBytes());
        
        Thread.sleep(1000L);
        
        base.stop();
    }
}
