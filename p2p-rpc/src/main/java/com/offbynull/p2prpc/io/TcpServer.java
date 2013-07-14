package com.offbynull.p2prpc.io;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.io.IOUtils;

public final class TcpServer implements Server {

    private long timeout;
    private InetSocketAddress listenAddress;
    private ServerCallback callback;
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
    public void start(ServerCallback callback) throws IOException {
        if (eventLoop != null) {
            throw new IllegalStateException();
        }

        this.callback = callback;
        
        eventLoop = new EventLoop();
        eventLoop.startAndWait();
    }

    @Override
    public void stop() throws IOException {
        if (eventLoop == null || eventLoop.isRunning()) {
            throw new IllegalStateException();
        }
        
        eventLoop.stopAndWait();
    }

    private class EventLoop extends AbstractExecutionThreadService {
        private Selector selector;
        private ServerSocketChannel serverChannel;
        private Map<SocketChannel, StreamedIoBuffers> clientChannelBuffers;
        private final AtomicReference<Thread> runningThread;

        public EventLoop() {
            this.runningThread = new AtomicReference<>(null);
        }

        @Override
        protected void startUp() throws Exception {
            clientChannelBuffers = new HashMap<>();
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
                selector.select();

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
                        clientChannel.register(selector, SelectionKey.OP_READ
                                | SelectionKey.OP_WRITE);
                        
                        StreamedIoBuffers buffers = new StreamedIoBuffers();
                        buffers.startReading();
                        clientChannelBuffers.put(clientChannel, buffers);
                    } else if (key.isReadable()) {
                        SocketChannel clientChannel =
                                (SocketChannel) key.channel();
                        StreamedIoBuffers buffers =
                                clientChannelBuffers.get(clientChannel);
                        
                        buffer.clear();
                        if (clientChannel.read(buffer) == -1) {
                            clientChannel.shutdownInput();
                            byte[] inData = buffers.finishReading();
                            byte[] outData = callback.incomingMessage(inData);
                            buffers.startWriting(outData);
                        } else {
                            buffers.addReadBlock(buffer);
                        }
                    } else if (key.isWritable()) {
                        SocketChannel clientChannel =
                                (SocketChannel) key.channel();
                        StreamedIoBuffers buffers =
                                clientChannelBuffers.get(clientChannel);
                        
                        buffer.clear();
                        buffers.getWriteBlock(buffer);
                        
                        if (buffer.position() == 0) {
                            clientChannel.shutdownOutput();
                        } else {
                            int amountWritten = clientChannel.write(buffer);
                            buffers.adjustWritePointer(amountWritten);
                        }
                    } else if (key.isConnectable()) {
                        ((SocketChannel)key.channel()).finishConnect(); 
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
            return "TCP Server Event Loop (" + listenAddress.toString() + ")";
        }
        
        @Override
        protected Executor executor() {
            return new Executor() {
                @Override
                public void execute(Runnable command) {
                    Thread thread = Executors.defaultThreadFactory().newThread(
                            command);
                    runningThread.compareAndSet(null, thread);

                    try {
                        thread.setName(serviceName());
                    } catch (SecurityException e) {
                        // OK if we can't set the name in this environment.
                    }
                    thread.start();
                }
            };
        }

        protected void interruptRunningThread() {
            Thread thread = runningThread.get();
            if (thread != null) {
                thread.interrupt();
            }
        }

    }
}
