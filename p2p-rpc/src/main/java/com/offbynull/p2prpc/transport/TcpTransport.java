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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.IOUtils;

public final class TcpTransport implements SessionedTransport<InetSocketAddress> {

    private InetSocketAddress listenAddress;
    private EventLoop eventLoop;

    public TcpTransport(int port) {
        this(new InetSocketAddress(port));
    }

    public TcpTransport(InetSocketAddress listenAddress) {
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
                System.out.println("Loop started");
                // get current time
                long currentTime = System.currentTimeMillis();
                
                
                // get requests waiting to go out, create socket for each request
                System.out.println("Getting requests");
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
                System.out.println("Getting responses");
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
                System.out.println("Selecting");
                try {
                    selector.select();
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }

                // stop if signalled
                System.out.println("Checking if stopped");
                if (stop.get()) {
                    return;
                }

                // go through selected keys
                System.out.println("Looping through keys");
                Iterator keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    System.out.println("Getting key");
                    SelectionKey key = (SelectionKey) keys.next();
                    keys.remove();

                    if (!key.isValid()) {
                        System.out.println("Key invalid");
                        continue;
                    }

                    if (key.isAcceptable()) {
                        System.out.println("Key is acceptable");
                        try {
                            acceptAndInitializeIncomingSocket();
                        } catch (RuntimeException | IOException e) {
                            // do nothing
                        }
                    } else if (key.isConnectable()) {
                        System.out.println("Key is connectable");
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
                        System.out.println("Key is readable");
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
                            killSocket(key, clientChannel, true);
                        }
                    } else if (key.isWritable()) {
                        System.out.println("Key is writable");
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
                            System.out.println(e);
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

                ChannelParameters params = new ChannelParameters(buffers, ClientChannelType.REMOTE_INITIATED, selectionKey, null, null);

                channelParametersMap.put(clientChannel, params);
            } catch (IOException | RuntimeException e) {
                killSocket(selectionKey, clientChannel, true);
            }
        }

        private void createAndInitializeOutgoingSocket(SendQueuedRequest queuedRequest) throws IOException {
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

    private static final class TcpRequestNotifier implements RequestNotifier<InetSocketAddress> {
        private LinkedBlockingQueue<RequestReceiver> handlers;
        private LinkedBlockingQueue<OutgoingResponse> queuedResponses;
        
        private TcpRequestNotifier() {
            handlers = new LinkedBlockingQueue<>();
            queuedResponses = new LinkedBlockingQueue<>();
        }

        @Override
        public void add(RequestReceiver<InetSocketAddress> e) {
            handlers.add(e);
        }

        @Override
        public void remove(RequestReceiver<InetSocketAddress> e) {
            handlers.remove(e);
        }

        private void notify(Collection<IncomingRequest> dataCollection) {
            RequestReceiver[] handlersArray = handlers.toArray(new RequestReceiver[0]);
            
            for (IncomingRequest incomingRequest : dataCollection) {
                for (RequestReceiver<InetSocketAddress> handler : handlersArray) { // to array to avoid locks
                    IncomingData<InetSocketAddress> data = incomingRequest.getRequest();
                    Selector selector = incomingRequest.getSelector();
                    SocketChannel channel = incomingRequest.getChannel();
                    
                    TcpResponseSender responseSender = new TcpResponseSender(queuedResponses, selector, channel);
                    
                    if (handler.requestArrived(data, responseSender)) {
                        break;
                    }
                }
            }
        }
        
        private void drainResponsesTo(Collection<OutgoingResponse> destination) {
            queuedResponses.drainTo(destination);
        }
    }
    
    
    private static final class IncomingRequest {
        private IncomingData<InetSocketAddress> request;
        private Selector selector;
        private SocketChannel channel;

        public IncomingRequest(IncomingData<InetSocketAddress> request, Selector selector, SocketChannel channel) {
            this.request = request;
            this.selector = selector;
            this.channel = channel;
        }

        public IncomingData<InetSocketAddress> getRequest() {
            return request;
        }

        public Selector getSelector() {
            return selector;
        }

        public SocketChannel getChannel() {
            return channel;
        }
        
    }
    
    
    private static final class TcpResponseSender implements ResponseSender<InetSocketAddress> {

        private LinkedBlockingQueue<OutgoingResponse> queue;
        private Selector selector;
        private SocketChannel channel;
        private AtomicBoolean consumed;

        public TcpResponseSender(LinkedBlockingQueue<OutgoingResponse> queue, Selector selector, SocketChannel channel) {
            this.queue = queue;
            this.selector = selector;
            this.channel = channel;
        }
        
        @Override
        public void sendResponse(OutgoingData<InetSocketAddress> data) {
            queue.add(new SendQueuedResponse(channel, data));
            selector.wakeup();
        }

        @Override
        public void killCommunication() {
            queue.add(new KillQueuedResponse(channel));
            selector.wakeup();
        }
        
    }
    
    private interface OutgoingResponse {
        
    }
    
    private static final class KillQueuedResponse implements OutgoingResponse {
        private SocketChannel channel;

        public KillQueuedResponse(SocketChannel channel) {
            this.channel = channel;
        }

        public SocketChannel getChannel() {
            return channel;
        }
    }
    
    private static final class SendQueuedResponse implements OutgoingResponse {
        private SocketChannel channel;
        private OutgoingData<InetSocketAddress> data;

        public SendQueuedResponse(SocketChannel channel, OutgoingData<InetSocketAddress> data) {
            this.channel = channel;
            this.data = data;
        }

        public SocketChannel getChannel() {
            return channel;
        }

        public OutgoingData<InetSocketAddress> getData() {
            return data;
        }

    }

    
    
    private static final class TcpRequestSender implements RequestSender<InetSocketAddress> {
        private Selector selector;
        private LinkedBlockingQueue<OutgoingRequest> outgoingData;
        private AtomicLong nextId;

        private TcpRequestSender(Selector selector) {
            this.selector = selector;
            this.outgoingData = new LinkedBlockingQueue<>();
            nextId = new AtomicLong();
        }
        
        @Override
        public RequestController sendRequest(OutgoingData<InetSocketAddress> data, ResponseReceiver<InetSocketAddress> receiver) {
            // TO FIX THIS FUNCTION...
            // assign an unique id to outgoingdata
            // pass that uniqueid to tcprequestcontroller
            
            long id = nextId.incrementAndGet();
            
            outgoingData.add(new SendQueuedRequest(data, receiver, id));
            selector.wakeup();
            
            return new TcpRequestController(id, selector, outgoingData);
        }
        
        private void drainTo(Collection<OutgoingRequest> destination) {
            outgoingData.drainTo(destination);
        }
    }
    
    private static final class TcpRequestController implements RequestController {
        private long id;
        private Selector selector;
        private LinkedBlockingQueue<OutgoingRequest> outgoingData;

        public TcpRequestController(long id, Selector selector, LinkedBlockingQueue<OutgoingRequest> outgoingData) {
            this.id = id;
            this.selector = selector;
            this.outgoingData = outgoingData;
        }



        @Override
        public void killCommunication() {
            outgoingData.add(new KillQueuedRequest(id));
            selector.wakeup();
        }
        
    }

    private interface OutgoingRequest {
        
    }
    
    private static final class SendQueuedRequest implements OutgoingRequest {

        private InetSocketAddress destination;
        private StreamIoBuffers buffers;
        private ResponseReceiver<InetSocketAddress> receiver;
        private long id;

        public SendQueuedRequest(OutgoingData<InetSocketAddress> data, ResponseReceiver<InetSocketAddress> receiver, long id) {
            this.destination = data.getTo();
            
            StreamIoBuffers streamIoBuffers = new StreamIoBuffers(Mode.WRITE_FIRST);
            streamIoBuffers.startWriting(data.getData());
                    
            this.buffers = streamIoBuffers;
            this.receiver = receiver;
            this.id = id;
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

        public long getId() {
            return id;
        }

    }

    private static final class KillQueuedRequest implements OutgoingRequest {

        private long id;

        public KillQueuedRequest(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
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
    
    public static void main(String[] args) throws Throwable {
        TcpTransport transport = new TcpTransport(12345);
        transport.start();
        
        RequestSender<InetSocketAddress> sender = transport.getRequestSender();
        sender.sendRequest(new OutgoingData<>(new InetSocketAddress("www.google.com", 80), "GET /\r\n\r\n".getBytes("US-ASCII")),
                new ResponseReceiver<InetSocketAddress>() {

            @Override
            public void responseArrived(IncomingData<InetSocketAddress> data) {
                System.out.println(data.getData().toString());
            }

            @Override
            public void communicationFailed() {
                System.out.println("HI");
            }
        });
        
        
        Thread.sleep(5000);
    }
}
