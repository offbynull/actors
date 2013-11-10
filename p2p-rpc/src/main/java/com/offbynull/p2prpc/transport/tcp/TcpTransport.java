package com.offbynull.p2prpc.transport.tcp;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.offbynull.p2prpc.transport.IncomingData;
import com.offbynull.p2prpc.transport.OutgoingData;
import com.offbynull.p2prpc.transport.SessionedTransport;
import com.offbynull.p2prpc.transport.StreamIoBuffers;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

public final class TcpTransport implements SessionedTransport<InetSocketAddress> {

    private InetSocketAddress listenAddress;
    private EventLoop eventLoop;
    
    public TcpTransport(int port) {
        this(new InetSocketAddress(port));
    }

    public TcpTransport(InetSocketAddress listenAddress) {
        Validate.notNull(listenAddress);
        
        this.listenAddress = listenAddress;
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
        private Map<Long, SocketChannel> sendQueueIdMap;
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
            channelParametersMap = new HashMap<>();
            sendQueueIdMap = new HashMap<>();
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

            LinkedList<IncomingRequest> pendingIncomingData = new LinkedList<>();
            LinkedList<OutgoingRequest> pendingRequestData = new LinkedList<>();
            LinkedList<OutgoingResponse> pendingResponseData = new LinkedList<>();
            while (true) {
                // get current time
                long currentTime = System.currentTimeMillis();
                
                
                // get requests waiting to go out, create socket for each request
                requestSender.drainTo(pendingRequestData);

                for (OutgoingRequest queuedRequest : pendingRequestData) {
                    try {
                        if (queuedRequest instanceof SendQueuedRequest) {
                            createAndInitializeOutgoingSocket((SendQueuedRequest) queuedRequest);
                        } else if (queuedRequest instanceof KillQueuedRequest) {
                            KillQueuedRequest kqr = (KillQueuedRequest) queuedRequest;

                            long id = kqr.getId();

                            killSocketByRequestId(id, false);
                        }
                    } catch (IOException | RuntimeException e) {
                        // do nothing
                    }
                }
                pendingRequestData.clear();

                
                
                // get responses waiting to go out
                requestNotifier.drainResponsesTo(pendingResponseData);

                for (OutgoingResponse queuedResponse : pendingResponseData) {
                    if (queuedResponse instanceof KillQueuedResponse) {
                        try {
                            KillQueuedResponse kqr = (KillQueuedResponse) queuedResponse;

                            SocketChannel channel = kqr.getChannel();
                            ChannelParameters params = channelParametersMap.get(channel);

                            if (params != null) {
                                killSocket(params.getSelectionKey(), channel, false);
                            }
                        } catch (RuntimeException e) {
                            // do nothing
                        }
                    } else if (queuedResponse instanceof SendQueuedResponse) {
                        try {
                            SendQueuedResponse sqr = (SendQueuedResponse) queuedResponse;

                            SocketChannel channel = sqr.getChannel();
                            OutgoingData<InetSocketAddress> data = sqr.getData();
                            ChannelParameters params = channelParametersMap.get(channel);

                            if (params != null) {
                                SelectionKey key = params.getSelectionKey();
                                StreamIoBuffers buffers = params.getBuffers();

                                buffers.startWriting(data.getData());
                                key.interestOps(SelectionKey.OP_WRITE);
                            }
                        } catch (RuntimeException e) {
                            // do nothing
                        }
                    } else {
                        throw new IllegalStateException();
                    }
                }
                pendingRequestData.clear();
                
                
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
                            e.printStackTrace();
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
                                    // should never happen
                                default:
                                    throw new IllegalStateException();
                            }
                        } catch (RuntimeException | IOException e) {
                            e.printStackTrace();
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
                                        IncomingRequest request = new IncomingRequest(incomingData, selector, clientChannel);
                                        pendingIncomingData.add(request);
                                        break;
                                    }
                                    default:
                                        throw new IllegalStateException();
                                }
                            }
                        } catch (RuntimeException | IOException e) {
                            e.printStackTrace();
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
                                
                                if (buffers.isEndOfWrite()) {
                                    clientChannel.shutdownOutput();
                                    buffers.finishWriting();

                                    switch (params.getType()) {
                                        case LOCAL_INITIATED: {
                                            buffers.startReading();
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
                            }
                        } catch (RuntimeException | IOException e) {
                            e.printStackTrace();
                            killSocket(key, clientChannel, true);
                        }
                    }
                }
                
                requestNotifier.notify(pendingIncomingData);
                pendingIncomingData.clear();
            }
        }

        private void killSocketByRequestId(long id, boolean triggerFailure) {
            SocketChannel channel = sendQueueIdMap.get(id);
            
            if (channel != null) {
                ChannelParameters params = channelParametersMap.get(channel);
                SelectionKey key = params.getSelectionKey();
                
                killSocket(key, channel, triggerFailure);
            }
        }
        
        private void killSocket(SelectionKey key, SocketChannel channel, boolean triggerFailure) {
            Validate.notNull(key);
            Validate.notNull(channel);
            
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
                clientChannel.socket().setTcpNoDelay(true);

                selectionKey = clientChannel.register(selector, SelectionKey.OP_READ); // no need to OP_CONNECT
                StreamIoBuffers buffers = new StreamIoBuffers(StreamIoBuffers.Mode.READ_FIRST);
                buffers.startReading();

                ChannelParameters params = new ChannelParameters(buffers, ClientChannelType.REMOTE_INITIATED, selectionKey, null, null);

                channelParametersMap.put(clientChannel, params);
            } catch (IOException | RuntimeException e) {
                killSocket(selectionKey, clientChannel, true);
                throw e;
            }
        }

        private void createAndInitializeOutgoingSocket(SendQueuedRequest queuedRequest) throws IOException {
            Validate.notNull(queuedRequest);
            
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

                long id = queuedRequest.getId();
                
                ChannelParameters params = new ChannelParameters(buffers, ClientChannelType.LOCAL_INITIATED, selectionKey,
                        queuedRequest.getReceiver(), id);
                
                sendQueueIdMap.put(id, clientChannel);
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
    
    
    private static final class ChannelParameters {

        private StreamIoBuffers buffers;
        private ClientChannelType type;
        private SelectionKey selectionKey;
        private ResponseReceiver<InetSocketAddress> receiver;
        private Long sendRequestId;

        public ChannelParameters(StreamIoBuffers buffers, ClientChannelType type, SelectionKey selectionKey,
                ResponseReceiver<InetSocketAddress> receiver, Long sendRequestId) {
            Validate.notNull(buffers);
            Validate.notNull(type);
            Validate.notNull(selectionKey);
            //Validate.notNull(receiver); // may be null
            //Validate.notNull(sendRequestId); // may be null
            
            this.buffers = buffers;
            this.type = type;
            this.selectionKey = selectionKey;
            this.receiver = receiver;
            this.sendRequestId = sendRequestId;
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

        public Long getSendRequestId() {
            return sendRequestId;
        }
        
    }
    
    private enum ClientChannelType {
        REMOTE_INITIATED,
        LOCAL_INITIATED
    }
}
