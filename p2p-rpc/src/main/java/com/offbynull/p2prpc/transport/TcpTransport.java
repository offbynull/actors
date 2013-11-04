package com.offbynull.p2prpc.transport;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.offbynull.p2prpc.transport.StreamIoBuffers.Mode;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.IOUtils;

public final class TcpTransport implements StreamTransport<InetSocketAddress> {

    private InetSocketAddress listenAddress;
    private EventLoop eventLoop;
    private long timeout;

    public TcpTransport(int port) {
        this(new InetSocketAddress(port), 10000L);
    }

    public TcpTransport(int port, long timeout) {
        this(new InetSocketAddress(port), timeout);
    }

    public TcpTransport(InetSocketAddress listenAddress, long timeout) {
        this.listenAddress = listenAddress;
        this.timeout = timeout;
    }
    
    @Override
    public void start() throws IOException {
        if (eventLoop != null) {
            throw new IllegalStateException();
        }

        eventLoop = new EventLoop();
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
    public RequestNotifier<InetSocketAddress> getRequestNotifier() {
        if (eventLoop == null || !eventLoop.isRunning()) {
            throw new IllegalStateException();
        }
        
        return eventLoop.getRequestNotifier();
    }

    @Override
    public RequestSender<InetSocketAddress> getRequestSender() {
        if (eventLoop == null || !eventLoop.isRunning()) {
            throw new IllegalStateException();
        }
        
        return eventLoop.getRequestSender();
    }
    
    private final class EventLoop extends AbstractExecutionThreadService {

        private TcpRequestNotifier requestNotifier;
        private TcpRequestSender requestSender;
        
        private Selector selector;
        private ServerSocketChannel serverChannel;
        private Map<SocketChannel, ChannelParameters> channelParametersMap;
        private AtomicBoolean stop;

        public EventLoop() throws IOException {
            try {
                selector = Selector.open();
                serverChannel = ServerSocketChannel.open();
            } catch (RuntimeException | IOException e) {
                IOUtils.closeQuietly(selector);
                IOUtils.closeQuietly(serverChannel);
                throw e;
            }
            
            requestNotifier = new TcpRequestNotifier();
            requestSender = new TcpRequestSender(selector);
        }

        @Override
        protected void startUp() throws Exception {
            Thread.currentThread().setDaemon(false);
            
            channelParametersMap = new HashMap<>();
            stop = new AtomicBoolean(false);
            try {
                serverChannel.configureBlocking(false);
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);
                serverChannel.socket().bind(listenAddress);
            } catch (RuntimeException | IOException e) {
                IOUtils.closeQuietly(selector);
                IOUtils.closeQuietly(serverChannel);
                throw e;
            }
        }

        public TcpRequestNotifier getRequestNotifier() {
            return requestNotifier;
        }

        public TcpRequestSender getRequestSender() {
            return requestSender;
        }
        
        @Override
        protected void run() {
            ByteBuffer tempBuffer = ByteBuffer.allocate(65535);

            LinkedList<IncomingData<InetSocketAddress>> pendingIncomingData = new LinkedList<>();
            LinkedList<QueuedRequest> pendingOutgoingData = new LinkedList<>();
            while (true) {
                // get current time
                long currentTime = System.currentTimeMillis();
                
                // get requests waiting to go out
                requestSender.drainTo(pendingOutgoingData);
                
                // create sockets for each request waiting to go out
                try {
                    for (QueuedRequest queuedRequest : pendingOutgoingData) {
                        createAndInitializeOutgoingSocket(queuedRequest);
                    }
                } catch (IOException | RuntimeException e) {
                    // do nothing
                }
                
                // select
                try {
                    selector.select();
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }

                // stop if signalled
                if (stop.get()) {
                    return;
                }

                // go through selected keys
                Iterator keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = (SelectionKey) keys.next();
                    keys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        try {
                            acceptAndInitializeIncomingSocket();
                        } catch (RuntimeException | IOException e) {
                            // do nothing
                        }
                    } else if (key.isConnectable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        
                        try {
                            ChannelParameters params = channelParametersMap.get(clientChannel);
                            
                            if (!clientChannel.finishConnect()) {
                                throw new RuntimeException();
                            }
                            
                            switch (params.getType()) {
                                case LOCAL_INITIATED:
                                    // if this is an outgoing message, first thing we want to do is write
                                    key.interestOps(SelectionKey.OP_WRITE);
                                    break;
                                case REMOTE_INITIATED:
                                    // if this is an incoming message, first thing we want to do is read
                                    key.interestOps(SelectionKey.OP_READ);
                                    break;
                                default:
                                    throw new IllegalStateException();
                            }
                        } catch (RuntimeException | IOException e) {
                            killSocket(key, clientChannel, true);
                        }
                    } else if (key.isReadable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        
                        try {
                            ChannelParameters params = channelParametersMap.get(clientChannel);
                            StreamIoBuffers buffers = params.getBuffers();

                            tempBuffer.clear();
                            if (clientChannel.read(tempBuffer) != -1) {
                                buffers.addReadBlock(tempBuffer);
                            } else {
                                clientChannel.shutdownInput();
                                byte[] inData = buffers.finishReading();

                                ResponseReceiver<InetSocketAddress> receiver = params.getReceiver();
                                InetSocketAddress from = (InetSocketAddress) clientChannel.getRemoteAddress();
                                IncomingData<InetSocketAddress> incomingData = new IncomingData<>(from, inData, currentTime);

                                switch (params.getType()) {
                                    case LOCAL_INITIATED: {
                                        receiver.responseArrived(incomingData);
                                        killSocket(key, clientChannel, false);
                                        break;
                                    }
                                    case REMOTE_INITIATED: {
                                        pendingIncomingData.add(incomingData);
                                        key.interestOps(SelectionKey.OP_WRITE);
                                        break;
                                    }
                                    default:
                                        throw new IllegalStateException();
                                }
                            }
                        } catch (RuntimeException | IOException e) {
                            killSocket(key, clientChannel, true);
                        }
                    } else if (key.isWritable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        
                        try {
                            ChannelParameters params = channelParametersMap.get(clientChannel);
                            StreamIoBuffers buffers = params.getBuffers();

                            tempBuffer.clear();
                            buffers.getWriteBlock(tempBuffer);

                            if (tempBuffer.limit() != 0) {
                                int amountWritten = clientChannel.write(tempBuffer);
                                buffers.adjustWritePointer(amountWritten);
                            } else {
                                clientChannel.shutdownOutput();
                                buffers.finishWriting();
                                
                                switch (params.getType()) {
                                    case LOCAL_INITIATED: {
                                        key.interestOps(SelectionKey.OP_READ);
                                        break;
                                    }
                                    case REMOTE_INITIATED: {
                                        killSocket(key, clientChannel, false);
                                        break;
                                    }
                                    default:
                                        throw new IllegalStateException();
                                }
                            }
                        } catch (RuntimeException | IOException e) {
                            killSocket(key, clientChannel, true);
                        }
                    }
                }
                
                requestNotifier.notify(pendingIncomingData);
                pendingIncomingData.clear();
            }
        }

        private void killSocket(SelectionKey key, SocketChannel channel, boolean triggerFailure) {
            if (channel != null) {
                IOUtils.closeQuietly(channel);
                ChannelParameters params = channelParametersMap.remove(channel);
                
                if (params != null && params.receiver != null && triggerFailure) {
                    params.receiver.communicationFailed();
                }
            }
            
            if (key != null) {
                key.cancel();
            }
        }
        
        private void acceptAndInitializeIncomingSocket() throws IOException {
            SocketChannel clientChannel = null;
            SelectionKey selectionKey = null;
            
            try {
                clientChannel = serverChannel.accept();
                clientChannel.configureBlocking(false);
                clientChannel.socket().setKeepAlive(true);
                clientChannel.socket().setReuseAddress(true);
                clientChannel.socket().setSoLinger(false, 0);
                clientChannel.socket().setSoTimeout(0);
                clientChannel.socket().setTcpNoDelay(false);

                selectionKey = clientChannel.register(selector, SelectionKey.OP_CONNECT);
                StreamIoBuffers buffers = new StreamIoBuffers(StreamIoBuffers.Mode.READ_FIRST);
                buffers.startReading();

                ChannelParameters params = new ChannelParameters(buffers, ClientChannelType.REMOTE_INITIATED, selectionKey, null);

                channelParametersMap.put(clientChannel, params);
            } catch (IOException | RuntimeException e) {
                killSocket(selectionKey, clientChannel, true);
            }
        }

        private void createAndInitializeOutgoingSocket(QueuedRequest queuedRequest) throws IOException {
            SocketChannel clientChannel = null;
            SelectionKey selectionKey = null;
            
            try {
                clientChannel = SocketChannel.open();

                clientChannel.configureBlocking(false);
                clientChannel.socket().setKeepAlive(true);
                clientChannel.socket().setReuseAddress(true);
                clientChannel.socket().setSoLinger(false, 0);
                clientChannel.socket().setSoTimeout(0);
                clientChannel.socket().setTcpNoDelay(false);

                selectionKey = clientChannel.register(selector, SelectionKey.OP_CONNECT);
                StreamIoBuffers buffers = queuedRequest.getBuffers();

                InetSocketAddress destinationAddress = queuedRequest.getDestination();

                ChannelParameters params = new ChannelParameters(buffers, ClientChannelType.LOCAL_INITIATED, selectionKey,
                        queuedRequest.getReceiver());

                channelParametersMap.put(clientChannel, params);

                clientChannel.connect(destinationAddress);
            } catch (IOException | RuntimeException e) {
                killSocket(selectionKey, clientChannel, true);
                throw e;
            }
        }

        @Override
        protected void shutDown() throws Exception {
            for (Map.Entry<SocketChannel, ChannelParameters> e
                    : channelParametersMap.entrySet()) {
                IOUtils.closeQuietly(e.getKey());
            }

            IOUtils.closeQuietly(selector);
            IOUtils.closeQuietly(serverChannel);
        }

        @Override
        protected String serviceName() {
            return TcpTransport.class.getSimpleName() + " Event Loop (" + listenAddress + ")";
        }

        @Override
        protected void triggerShutdown() {
            stop.set(true);
            selector.wakeup();
        }
    }

    public static final class TcpRequestNotifier implements RequestNotifier<InetSocketAddress> {
        private LinkedBlockingQueue<RequestReceiver> handlers;
        
        private TcpRequestNotifier() {
            handlers = new LinkedBlockingQueue<>();
        }

        @Override
        public void add(RequestReceiver<InetSocketAddress> e) {
            handlers.add(e);
        }

        @Override
        public void remove(RequestReceiver<InetSocketAddress> e) {
            handlers.remove(e);
        }
        
        private void notify(IncomingData<InetSocketAddress> ... dataCollection) {
            notify(Arrays.asList(dataCollection));
        }

        private void notify(Collection<IncomingData<InetSocketAddress>> dataCollection) {
            RequestReceiver[] handlersArray = handlers.toArray(new RequestReceiver[0]);
            
            for (IncomingData<InetSocketAddress> data : dataCollection) {
                for (RequestReceiver<InetSocketAddress> handler : handlersArray) { // to array to avoid locks
                    if (handler.requestArrived(data)) {
                        break;
                    }
                }
            }
        }
    }
    
    public static final class TcpRequestSender implements RequestSender<InetSocketAddress> {
        private Selector selector;
        private LinkedBlockingQueue<QueuedRequest> outgoingData;

        private TcpRequestSender(Selector selector) {
            this.selector = selector;
            this.outgoingData = new LinkedBlockingQueue<>();
        }
        
        @Override
        public void sendRequest(OutgoingData<InetSocketAddress> data, ResponseReceiver<InetSocketAddress> receiver) {
            outgoingData.add(new QueuedRequest(data, receiver));
            selector.wakeup();
        }
        
        private void drainTo(Collection<QueuedRequest> destination) {
            outgoingData.drainTo(destination);
        }
    }

    private static final class QueuedRequest {

        private InetSocketAddress destination;
        private StreamIoBuffers buffers;
        private ResponseReceiver<InetSocketAddress> receiver;

        public QueuedRequest(OutgoingData<InetSocketAddress> data, ResponseReceiver<InetSocketAddress> receiver) {
            this.destination = data.getTo();
            
            StreamIoBuffers streamIoBuffers = new StreamIoBuffers(Mode.WRITE_FIRST);
            streamIoBuffers.startWriting(data.getData());
                    
            this.buffers = streamIoBuffers;
            this.receiver = receiver;
        }

        public InetSocketAddress getDestination() {
            return destination;
        }

        public StreamIoBuffers getBuffers() {
            return buffers;
        }

        public ResponseReceiver<InetSocketAddress> getReceiver() {
            return receiver;
        }

    }
    
    private static final class ChannelParameters {

        private StreamIoBuffers buffers;
        private ClientChannelType type;
        private SelectionKey selectionKey;
        private ResponseReceiver<InetSocketAddress> receiver;

        public ChannelParameters(StreamIoBuffers buffers, ClientChannelType type, SelectionKey selectionKey,
                ResponseReceiver<InetSocketAddress> receiver) {
            this.buffers = buffers;
            this.type = type;
            this.selectionKey = selectionKey;
            this.receiver = receiver;
        }

        public StreamIoBuffers getBuffers() {
            return buffers;
        }

        public ClientChannelType getType() {
            return type;
        }

        public SelectionKey getSelectionKey() {
            return selectionKey;
        }

        public ResponseReceiver<InetSocketAddress> getReceiver() {
            return receiver;
        }
        
    }
    
    private enum ClientChannelType {
        REMOTE_INITIATED,
        LOCAL_INITIATED
    }
}
