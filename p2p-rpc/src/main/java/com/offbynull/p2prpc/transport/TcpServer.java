package com.offbynull.p2prpc.transport;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.offbynull.p2prpc.transport.StreamedIoBuffers.Mode;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.IOUtils;

public final class TcpServer implements Server<SocketAddress> {

    private long timeout;
    private InetSocketAddress listenAddress;
    private EventLoop eventLoop;

    public TcpServer(int port) {
        this(new InetSocketAddress(port), 10000L);
    }

    public TcpServer(int port, long timeout) {
        this(new InetSocketAddress(port), timeout);
    }

    public TcpServer(InetSocketAddress listenAddress, long timeout) {
        this.timeout = timeout;
        this.listenAddress = listenAddress;
    }

    @Override
    public void start(ServerMessageCallback<SocketAddress> callback) throws IOException {
        if (eventLoop != null) {
            throw new IllegalStateException();
        }

        eventLoop = new EventLoop(timeout, listenAddress, callback);
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
        private InetSocketAddress listenAddress;
        private ServerMessageCallback callback;
        
        private TimeoutTracker<SocketChannel> timeoutTracker;
        
        private Selector selector;
        private ServerSocketChannel serverChannel;
        private Map<SocketChannel, ClientChannelParams> clientChannelMap;
        private AtomicBoolean stop;

        public EventLoop(long timeout, InetSocketAddress listenAddress, ServerMessageCallback callback) {
            this.timeout = timeout;
            this.listenAddress = listenAddress;
            this.callback = callback;
        }

        @Override
        protected void startUp() throws Exception {
            clientChannelMap = new HashMap<>();
            timeoutTracker = new TimeoutTracker<>();
            stop = new AtomicBoolean(false);
            try {
                selector = Selector.open();
                serverChannel = ServerSocketChannel.open();

                serverChannel.configureBlocking(false);
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);
                serverChannel.socket().bind(listenAddress);
            } catch (Exception e) {
                IOUtils.closeQuietly(selector);
                IOUtils.closeQuietly(serverChannel);
                throw e;
            }
        }

        @Override
        protected void run() throws Exception {
            ByteBuffer buffer = ByteBuffer.allocate(8192);

            while (true) {
                long currentTime = System.currentTimeMillis();
                long nextAwakeTime = timeoutTracker.getNextQueryTime();
                long durationUntilNextAwake = Math.max(nextAwakeTime - currentTime, 1L); // must be > 0, 0 = forever for select
                
                selector.select(durationUntilNextAwake);

                if (stop.get()) {
                    return;
                }
                
                currentTime = System.currentTimeMillis(); // update

                Iterator keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = (SelectionKey) keys.next();
                    keys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        SocketChannel clientChannel = serverChannel.accept();
                        clientChannel.configureBlocking(false);
                        clientChannel.socket().setKeepAlive(true);
                        clientChannel.socket().setReuseAddress(true);
                        clientChannel.socket().setSoLinger(false, 0);
                        clientChannel.socket().setSoTimeout(0);
                        clientChannel.socket().setTcpNoDelay(true);

                        SelectionKey selectionKey = clientChannel.register(
                                selector, SelectionKey.OP_READ);
                        StreamedIoBuffers buffers =
                                new StreamedIoBuffers(Mode.READ_FIRST);

                        ClientChannelParams params = new ClientChannelParams(
                                buffers, selectionKey);
                        buffers.startReading();
                        
                        clientChannelMap.put(clientChannel, params);
                        
                        long killTime = currentTime + timeout;
                        timeoutTracker.add(clientChannel, killTime);
                    } else if (key.isReadable()) {
                        SocketChannel clientChannel =
                                (SocketChannel) key.channel();
                        ClientChannelParams params =
                                clientChannelMap.get(clientChannel);
                        SocketAddress from = clientChannel.getRemoteAddress();

                        StreamedIoBuffers buffers = params.getBuffers();

                        buffer.clear();
                        if (clientChannel.read(buffer) == -1) {
                            clientChannel.shutdownInput();
                            byte[] inData = buffers.finishReading();
                            ServerResponseCallback responseCallback =
                                    new ResponseCallback(clientChannel, params);
                            callback.messageArrived(from, inData, responseCallback);
                        } else {
                            buffers.addReadBlock(buffer);
                        }
                    } else if (key.isWritable()) {
                        SocketChannel clientChannel =
                                (SocketChannel) key.channel();
                        ClientChannelParams params =
                                clientChannelMap.get(clientChannel);

                        StreamedIoBuffers buffers = params.getBuffers();

                        buffer.clear();
                        buffers.getWriteBlock(buffer);

                        if (buffer.limit() == 0) {
                            clientChannel.shutdownOutput();
                            IOUtils.closeQuietly(clientChannel);
                            clientChannelMap.remove(clientChannel);
                            timeoutTracker.remove(clientChannel);
                        } else {
                            int amountWritten = clientChannel.write(buffer);
                            buffers.adjustWritePointer(amountWritten);
                        }
                    }
                }
            }
        }

        @Override
        protected void shutDown() throws Exception {
            for (Entry<SocketChannel, ClientChannelParams> e
                    : clientChannelMap.entrySet()) {
                IOUtils.closeQuietly(e.getKey());
            }

            IOUtils.closeQuietly(selector);
            IOUtils.closeQuietly(serverChannel);
        }

        @Override
        protected String serviceName() {
            return "TCP Server Event Loop (" + listenAddress.toString() + ")";
        }

        @Override
        protected void triggerShutdown() {
            stop.set(true);
            selector.wakeup();
        }
    }

    private static final class ResponseCallback
            implements ServerResponseCallback {

        private SocketChannel channel;
        private ClientChannelParams params;

        public ResponseCallback(SocketChannel channel,
                ClientChannelParams params) {
            this.channel = channel;
            this.params = params;
        }

        @Override
        public void responseReady(byte[] data) {
            StreamedIoBuffers buffers = params.getBuffers();
            SelectionKey selectionKey = params.getSelectionKey();
            
            buffers.startWriting(data);
            selectionKey.interestOps(SelectionKey.OP_WRITE);
        }

        @Override
        public void terminate() {
            IOUtils.closeQuietly(channel);
        }
    }

    private static final class ClientChannelParams {

        private StreamedIoBuffers buffers;
        private SelectionKey selectionKey;

        public ClientChannelParams(StreamedIoBuffers buffers,
                SelectionKey selectionKey) {
            this.buffers = buffers;
            this.selectionKey = selectionKey;
        }

        public StreamedIoBuffers getBuffers() {
            return buffers;
        }

        public SelectionKey getSelectionKey() {
            return selectionKey;
        }
    }

//    public static void main(String[] args) throws Throwable {
//        ServerMessageCallback<SocketAddress> callback = new ServerMessageCallback<SocketAddress>() {
//            @Override
//            public void messageArrived(SocketAddress from, byte[] data,
//                    ServerResponseCallback responseCallback) {
//                responseCallback.responseReady("OUTPUT".getBytes());
//            }
//        };
//
//        TcpServer tcpServer = new TcpServer(12345, 999999L);
//        tcpServer.start(callback);
//
//        TcpClient tcpClient = new TcpClient(999999L);
//        tcpClient.start();
//        byte[] data = tcpClient.send(new InetSocketAddress("localhost", 12345),
//                "GET /\r\n\r\n".getBytes());
//        System.out.println(new String(data));
//        tcpClient.stop();
//
//        tcpServer.stop();
//    }
}
