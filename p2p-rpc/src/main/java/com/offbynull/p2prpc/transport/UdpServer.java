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

public final class UdpServer implements Server<SocketAddress> {
    private long timeout;
    private int bufferSize;
    private InetSocketAddress listenAddress;
    private EventLoop eventLoop;

    public UdpServer(int port) {
        this(new InetSocketAddress(port), 10000L);
    }

    public UdpServer(int port, long timeout) {
        this(new InetSocketAddress(port), timeout);
    }

    public UdpServer(InetSocketAddress listenAddress, long timeout) {
        this(listenAddress, timeout, 65535);
    }

    public UdpServer(InetSocketAddress listenAddress, long timeout,
            int bufferSize) {
        this.timeout = timeout;
        this.listenAddress = listenAddress;
        this.bufferSize = bufferSize;
    }
    
    @Override
    public void start(ServerMessageCallback<SocketAddress> callback) throws IOException {
        if (eventLoop != null) {
            throw new IllegalStateException();
        }

        eventLoop = new EventLoop(timeout, bufferSize, listenAddress, callback);
        eventLoop.startAndWait();
    }

    @Override
    public void stop() throws IOException {
        if (eventLoop == null || !eventLoop.isRunning()) {
            throw new IllegalStateException();
        }

        eventLoop.stopAndWait();
    }

    private static final class EventLoop extends AbstractExecutionThreadService {

        private long timeout;
        private int bufferSize;
        private InetSocketAddress listenAddress;
        private ServerMessageCallback callback;

        private Selector selector;
        private DatagramChannel serverChannel;
        private AtomicBoolean stop;

        public EventLoop(long timeout, int bufferSize, InetSocketAddress listenAddress, ServerMessageCallback callback) {
            this.timeout = timeout;
            this.bufferSize = bufferSize;
            this.listenAddress = listenAddress;
            this.callback = callback;
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
                        
                        ServerResponseCallback responseCallback =
                                    new ResponseCallback(channel, from);
                        
                        callback.messageArrived(from, inData, responseCallback);
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
            return "UDP Server Event Loop (" + listenAddress.toString() + ")";
        }

        @Override
        protected void triggerShutdown() {
            stop.set(true);
            selector.wakeup();
        }
    }

    private static final class ResponseCallback
            implements ServerResponseCallback {

        private DatagramChannel channel;
        private SocketAddress requester;

        public ResponseCallback(DatagramChannel channel, SocketAddress requester) {
            this.channel = channel;
            this.requester = requester;
        }

        @Override
        public void responseCompleted(byte[] data) {
            try {
                channel.send(ByteBuffer.wrap(data), requester);
            } catch (IOException ioe) {
                // send failed. do nothing.
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        ServerMessageCallback<SocketAddress> callback = new ServerMessageCallback<SocketAddress>() {
            @Override
            public void messageArrived(SocketAddress from, byte[] data,
                    ServerResponseCallback responseCallback) {
                responseCallback.responseCompleted("OUTPUT".getBytes());
            }
        };

        UdpServer udpServer = new UdpServer(12345);
        udpServer.start(callback);

        UdpClient udpClient = new UdpClient();
        udpClient.start();
        byte[] data = udpClient.send(new InetSocketAddress("localhost", 12345),
                "GET /\r\n\r\n".getBytes());
        System.out.println(new String(data));
        udpClient.stop();

        udpServer.stop();
    }
}
