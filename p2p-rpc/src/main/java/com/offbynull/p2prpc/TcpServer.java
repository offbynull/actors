package com.offbynull.p2prpc;

import com.offbynull.p2prpc.invoke.Invoker;
import com.offbynull.p2prpc.invoke.InvokerCallback;
import com.offbynull.p2prpc.invoke.InvokeData;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    private State state = State.INIT;
    private InetSocketAddress listenAddress;
    private long timeout;
    private Invoker invoker;
    private final AtomicReference<Thread> runningThread;

    public TcpServer(int port) {
        this(new InetSocketAddress(port), 10000L);
    }

    public TcpServer(int port, long timeout) {
        this(new InetSocketAddress(port), timeout);
    }

    public TcpServer(InetSocketAddress listenAddress, long timeout) {
        this.timeout = timeout;
        this.listenAddress = listenAddress;
        this.runningThread = new AtomicReference<>(null);
    }

    @Override
    public void start(Invoker invoker) throws IOException {
        if (state != State.STARTED) {
            throw new IllegalStateException();
        }

        this.invoker = invoker;
        
        eventLoopThread = new Thread(new EventLoopRunnable());
    }

    @Override
    public void stop() throws IOException {
        if (state != State.STOPPED) {
            throw new IllegalStateException();
        }
        
        
    }

    private class EventLoop extends AbstractExecutionThreadService {
        private Selector selector;
        private ServerSocketChannel serverChannel;
        private Map<SocketChannel, ClientData> clientChannels;

        @Override
        protected void startUp() throws Exception {
            clientChannels = new HashMap<>();
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
                        ClientData clientData = new ClientData();
                        clientChannels.put(clientChannel, clientData);
                    } else if (key.isReadable()) {
                        SocketChannel clientChannel =
                                (SocketChannel) key.channel();
                        ClientData clientData =
                                clientChannels.get(clientChannel);
                        
                        if (clientChannel.read(buffer) == -1) {
                            clientChannel.shutdownInput();
                            InvokeData invokeData =
                                    clientData.dumpIncomingData();
                            invoker.invoke(invokeData, callback);
                        }

                        clientData.appendIncomingData(buffer);
                        buffer.clear();
                    } else if (key.isWritable()) {
                        SocketChannel clientChannel =
                                (SocketChannel) key.channel();
                        ClientData clientData =
                                clientChannels.get(clientChannel);
                        
                        buffer.clear();
                        clientData.grabOutgoingData(buffer);
                        clientChannel.write(buffer);
                        
                        if (buffer.remaining() == 0) {
                            clientChannel.shutdownOutput();
                        }
                        
                        buffer.clear();
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

    private final class CustomInvokerCallback implements InvokerCallback {
        private SocketChannel clientChannel;
        private ClientData clientData;

        public CustomInvokerCallback(SocketChannel clientChannel,
                ClientData clientData) {
            this.clientChannel = clientChannel;
            this.clientData = clientData;
        }

        @Override
        public void methodReturned(Object retVal) {
        }

        @Override
        public void methodErrored(Throwable throwable) {
        }

        @Override
        public void invokationErrored(Throwable throwable) {
        }
    }

    private final class ClientData {
        private ByteArrayOutputStream incomingBuffer;
        private ByteBuffer outgoingBuffer;
        
        public void appendIncomingData(ByteBuffer buffer) {
            incomingBuffer.write(buffer.array(), 0, buffer.position());
        }
        
        public InvokeData dumpIncomingData() {
            byte[] data = incomingBuffer.toByteArray();
            incomingBuffer = null;
            return (InvokeData) xstream.fromXML(new ByteArrayInputStream(data));
        }
        
        public void grabOutgoingData(ByteBuffer buffer) {
            
            buffer.put(outgoingBuffer., offset, length);
        }
    }
    
    private enum State {

        INIT,
        STARTED,
        STOPPED
    }
}
