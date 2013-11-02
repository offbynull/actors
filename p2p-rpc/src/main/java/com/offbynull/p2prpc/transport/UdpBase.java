package com.offbynull.p2prpc.transport;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
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

    private static final class EventLoop extends AbstractExecutionThreadService {

        private int bufferSize;
        private InetSocketAddress listenAddress;

        private Selector selector;
        private DatagramChannel serverChannel;
        private AtomicBoolean stop;

        public EventLoop(int bufferSize, InetSocketAddress listenAddress) {
            this.bufferSize = bufferSize;
            this.listenAddress = listenAddress;
        }
        
        @Override
        protected void startUp() throws Exception {
            stop = new AtomicBoolean(false);
            try {
                selector = Selector.open();
                serverChannel = DatagramChannel.open();

                serverChannel.configureBlocking(false);
                serverChannel.register(selector, SelectionKey.OP_READ);
                serverChannel.socket().bind(listenAddress);
            } catch (Exception e) {
                IOUtils.closeQuietly(selector);
                IOUtils.closeQuietly(serverChannel);
                throw e;
            }
        }

        @Override
        protected void run() throws Exception {
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

            while (true) {
                selector.select();

                if (stop.get()) {
                    return;
                }

                Iterator keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = (SelectionKey) keys.next();
                    keys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isReadable()) {
                        DatagramChannel channel = (DatagramChannel) key.channel();
                        SocketAddress from = channel.receive(buffer);

                        buffer.flip();
                        byte[] inData = new byte[buffer.remaining()];
                        buffer.get(inData);
                        buffer.clear();
                        
//                        ServerResponseCallback responseCallback =
//                                    new ResponseCallback(channel, from);
//                        
//                        callback.messageArrived(from, inData, responseCallback);
                    }
                }
            }
        }

        @Override
        protected void shutDown() throws Exception {
            IOUtils.closeQuietly(selector);
            IOUtils.closeQuietly(serverChannel);
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
}
