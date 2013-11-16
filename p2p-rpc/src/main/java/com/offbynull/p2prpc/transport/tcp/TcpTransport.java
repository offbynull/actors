package com.offbynull.p2prpc.transport.tcp;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.offbynull.p2prpc.transport.IncomingData;
import com.offbynull.p2prpc.transport.OutgoingData;
import com.offbynull.p2prpc.transport.SessionedTransport;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

public final class TcpTransport implements SessionedTransport<InetSocketAddress> {

    private InetSocketAddress listenAddress;
    private EventLoop eventLoop;
    private int readLimit;
    private int writeLimit;
    private Lock accessLock;

    public TcpTransport(int readLimit, int writeLimit) {
        this(null, readLimit, writeLimit);
    }

    public TcpTransport(int port, int readLimit, int writeLimit) {
        this(new InetSocketAddress(port), readLimit, writeLimit);
    }

    public TcpTransport(InetSocketAddress listenAddress, int readLimit, int writeLimit) {
        //Validate.notNull(listenAddress); // null = no server
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, readLimit);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, writeLimit);

        this.listenAddress = listenAddress;
        this.readLimit = readLimit;
        this.writeLimit = writeLimit;
        accessLock = new ReentrantLock();
    }

    @Override
    public void start() throws IOException {
        accessLock.lock();
        try {
            if (eventLoop != null) {
                throw new IllegalStateException();
            }

            eventLoop = new EventLoop();
            eventLoop.startAndWait();
        } finally {
            accessLock.unlock();
        }
    }

    @Override
    public void stop() throws IOException {
        accessLock.lock();
        try {
            if (eventLoop == null || !eventLoop.isRunning()) {
                throw new IllegalStateException();
            }

            eventLoop.stopAndWait();
        } finally {
            accessLock.unlock();
        }
    }

    @Override
    public RequestNotifier<InetSocketAddress> getRequestNotifier() {
        accessLock.lock();
        try {
            if (eventLoop == null || !eventLoop.isRunning()) {
                throw new IllegalStateException();
            }

            return eventLoop.getRequestNotifier();
        } finally {
            accessLock.unlock();
        }
    }

    @Override
    public RequestSender<InetSocketAddress> getRequestSender() {
        accessLock.lock();
        try {
            if (eventLoop == null || !eventLoop.isRunning()) {
                throw new IllegalStateException();
            }

            return eventLoop.getRequestSender();
        } finally {
            accessLock.unlock();
        }
    }

    private final class EventLoop extends AbstractExecutionThreadService {

        private TcpRequestNotifier requestNotifier;
        private TcpRequestSender requestSender;

        private Selector selector;
        private ServerSocketChannel serverChannel;
        private Map<SocketChannel, ChannelInfo> channelInfoMap;
        private Map<Long, ChannelInfo> sendQueueIdInfoMap;
        private AtomicBoolean stop;

        LinkedBlockingQueue<Command> commandQueue;

        public EventLoop() throws IOException {
            try {
                selector = Selector.open();
                if (listenAddress != null) {
                    serverChannel = ServerSocketChannel.open();
                }
            } catch (RuntimeException | IOException e) {
                IOUtils.closeQuietly(selector);
                IOUtils.closeQuietly(serverChannel);
                throw e;
            }

            commandQueue = new LinkedBlockingQueue<>();

            requestNotifier = new TcpRequestNotifier(commandQueue);
            requestSender = new TcpRequestSender(selector, commandQueue);
        }

        @Override
        protected void startUp() throws Exception {
            channelInfoMap = new HashMap<>();
            sendQueueIdInfoMap = new HashMap<>();
            stop = new AtomicBoolean(false);
            try {
                if (listenAddress != null) {
                    serverChannel.configureBlocking(false);
                    serverChannel.register(selector, SelectionKey.OP_ACCEPT);
                    serverChannel.socket().bind(listenAddress);
                }
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

            LinkedList<Event> eventQueue = new LinkedList<>();
            LinkedList<Command> dumpedCommandQueue = new LinkedList<>();
            while (true) {
                // get current time
                long currentTime = System.currentTimeMillis();

                // dump commands and loop over them
                commandQueue.drainTo(dumpedCommandQueue);

                for (Command command : dumpedCommandQueue) {
                    try {
                        if (command instanceof CommandSendRequest) {
                            createAndInitializeOutgoingSocket((CommandSendRequest) command);
                        } else if (command instanceof CommandKillQueued) {
                            CommandKillQueued commandKq = (CommandKillQueued) command;
                            killSocketBySendQueueId(commandKq.getId());
                        } else if (command instanceof CommandKillEstablished) {
                            CommandKillEstablished commandKe = (CommandKillEstablished) command;
                            killSocketByChannel(commandKe.getChannel());
                        } else if (command instanceof CommandSendResponse) {
                            CommandSendResponse commandSr = (CommandSendResponse) command;

                            SocketChannel channel = commandSr.getChannel();
                            OutgoingData<InetSocketAddress> data = commandSr.getData();
                            ChannelInfo info = channelInfoMap.get(channel);

                            if (info != null) {
                                SelectionKey key = info.getSelectionKey();
                                StreamIoBuffers buffers = info.getBuffers();

                                buffers.startWriting(data.getData());
                                key.interestOps(SelectionKey.OP_WRITE);
                            }
                        }
                    } catch (IOException | RuntimeException e) {
                        // do nothing
                    }
                }
                dumpedCommandQueue.clear();

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
                            ChannelInfo info = acceptAndInitializeIncomingSocket();

                            SocketChannel channel = info.getChannel();
                            InetSocketAddress from = (InetSocketAddress) channel.getRemoteAddress();

                            EventLinkEstablished eventLe = new EventLinkEstablished(from, selector, channel);
                            eventQueue.add(eventLe);
                        } catch (RuntimeException | IOException e) {
                            e.printStackTrace();
                            // do nothing
                        }
                    } else if (key.isConnectable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();

                        try {
                            ChannelInfo params = channelInfoMap.get(clientChannel);

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
                            killSocket(clientChannel, e);
                        }
                    } else if (key.isReadable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();

                        try {
                            ChannelInfo params = channelInfoMap.get(clientChannel);
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
                                        killSocketByChannel(clientChannel);
                                        break;
                                    }
                                    case REMOTE_INITIATED: {
                                        EventRequestArrived eventRa = new EventRequestArrived(incomingData, selector, clientChannel);
                                        eventQueue.add(eventRa);
                                        key.interestOps(0); // don't need to read anymore, when we get a response, we register op_write
                                        break;
                                    }
                                    default:
                                        throw new IllegalStateException();
                                }
                            }
                        } catch (RuntimeException | IOException e) {
                            e.printStackTrace();
                            killSocket(clientChannel, e);
                        }
                    } else if (key.isWritable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();

                        try {
                            ChannelInfo params = channelInfoMap.get(clientChannel);
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
                                            killSocketByChannel(clientChannel);
                                            break;
                                        }
                                        default:
                                            throw new IllegalStateException();
                                    }
                                }
                            }
                        } catch (RuntimeException | IOException e) {
                            e.printStackTrace();
                            killSocket(clientChannel, e);
                        }
                    }
                }

                requestNotifier.notify(eventQueue);
                eventQueue.clear();
            }
        }

        private void killSocketByChannel(SocketChannel channel) {
            ChannelInfo info = channelInfoMap.get(channel);

            if (info == null) {
                return;
            }

            killSocket(channel, null);
        }

        private void killSocketBySendQueueId(long id) {
            ChannelInfo info = sendQueueIdInfoMap.get(id);

            if (info == null) {
                return;
            }

            SocketChannel channel = info.getChannel();
            killSocket(channel, null);
        }

        private void killSocket(SocketChannel channel, Throwable error) {
            Validate.notNull(channel);

            ChannelInfo info = channelInfoMap.remove(channel);
            if (info == null) {
                return;
            }

            Long id = info.getSendRequestId();
            if (id != null) {
                sendQueueIdInfoMap.remove(id);
            }

            SelectionKey key = info.getSelectionKey();
            key.cancel();

            IOUtils.closeQuietly(channel);
            if (info.receiver != null && error != null) {
                info.receiver.internalFailure(error);
            }
        }

        private ChannelInfo acceptAndInitializeIncomingSocket() throws IOException {
            SocketChannel clientChannel = null;
            SelectionKey selectionKey;

            try {
                clientChannel = serverChannel.accept();
                clientChannel.configureBlocking(false);
                clientChannel.socket().setKeepAlive(true);
                clientChannel.socket().setReuseAddress(true);
                clientChannel.socket().setSoLinger(false, 0);
                clientChannel.socket().setSoTimeout(0);
                clientChannel.socket().setTcpNoDelay(true);

                selectionKey = clientChannel.register(selector, SelectionKey.OP_READ); // no need to OP_CONNECT
                StreamIoBuffers buffers = new StreamIoBuffers(StreamIoBuffers.Mode.READ_FIRST, readLimit, writeLimit);
                buffers.startReading();

                ChannelInfo info = new ChannelInfo(clientChannel, buffers, ClientChannelType.REMOTE_INITIATED, selectionKey,
                        null, null);

                channelInfoMap.put(clientChannel, info);

                return info;
            } catch (IOException | RuntimeException e) {
                if (clientChannel != null) {
                    killSocket(clientChannel, e);
                }
                throw e;
            }
        }

        private void createAndInitializeOutgoingSocket(CommandSendRequest queuedRequest) throws IOException {
            Validate.notNull(queuedRequest);

            SocketChannel clientChannel = null;
            SelectionKey selectionKey;

            try {
                clientChannel = SocketChannel.open();

                clientChannel.configureBlocking(false);
                clientChannel.socket().setKeepAlive(true);
                clientChannel.socket().setReuseAddress(true);
                clientChannel.socket().setSoLinger(false, 0);
                clientChannel.socket().setSoTimeout(0);
                clientChannel.socket().setTcpNoDelay(false);

                selectionKey = clientChannel.register(selector, SelectionKey.OP_CONNECT);

                ByteBuffer data = queuedRequest.getData();
                InetSocketAddress destinationAddress = queuedRequest.getDestination();

                StreamIoBuffers buffers = new StreamIoBuffers(StreamIoBuffers.Mode.WRITE_FIRST, readLimit, writeLimit);
                buffers.startWriting(data);

                long id = queuedRequest.getId();

                ChannelInfo info = new ChannelInfo(clientChannel, buffers, ClientChannelType.LOCAL_INITIATED, selectionKey,
                        queuedRequest.getReceiver(), id);

                sendQueueIdInfoMap.put(id, info);
                channelInfoMap.put(clientChannel, info);

                clientChannel.connect(destinationAddress);
            } catch (IOException | RuntimeException e) {
                if (clientChannel != null) {
                    killSocket(clientChannel, e);
                }
                throw e;
            }
        }

        @Override
        protected void shutDown() throws Exception {
            for (Map.Entry<SocketChannel, ChannelInfo> e
                    : channelInfoMap.entrySet()) {
                IOUtils.closeQuietly(e.getKey());
            }

            IOUtils.closeQuietly(selector);
            IOUtils.closeQuietly(serverChannel);
        }

        @Override
        protected String serviceName() {
            return TcpTransport.class.getSimpleName() + " Event Loop (" + (listenAddress == null ? "N/A" : listenAddress) + ")";
        }

        @Override
        protected void triggerShutdown() {
            stop.set(true);
            selector.wakeup();
        }
    }

    private static final class ChannelInfo {

        private SocketChannel channel;
        private StreamIoBuffers buffers;
        private ClientChannelType type;
        private SelectionKey selectionKey;
        private ResponseReceiver<InetSocketAddress> receiver;
        private Long sendRequestId;

        public ChannelInfo(SocketChannel channel, StreamIoBuffers buffers, ClientChannelType type, SelectionKey selectionKey,
                ResponseReceiver<InetSocketAddress> receiver, Long sendRequestId) {
            Validate.notNull(channel);
            Validate.notNull(buffers);
            Validate.notNull(type);
            Validate.notNull(selectionKey);
            //Validate.notNull(receiver); // may be null
            //Validate.notNull(sendRequestId); // may be null

            this.channel = channel;
            this.buffers = buffers;
            this.type = type;
            this.selectionKey = selectionKey;
            this.receiver = receiver;
            this.sendRequestId = sendRequestId;
        }

        public SocketChannel getChannel() {
            return channel;
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
