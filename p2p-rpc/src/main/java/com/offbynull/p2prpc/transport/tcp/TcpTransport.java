package com.offbynull.p2prpc.transport.tcp;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.offbynull.p2prpc.transport.IncomingMessage;
import com.offbynull.p2prpc.transport.IncomingMessageListener;
import com.offbynull.p2prpc.transport.IncomingResponse;
import com.offbynull.p2prpc.transport.OutgoingMessage;
import com.offbynull.p2prpc.transport.OutgoingMessageResponseListener;
import com.offbynull.p2prpc.transport.OutgoingResponse;
import com.offbynull.p2prpc.transport.Transport;
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

public final class TcpTransport implements Transport {

    private InetSocketAddress listenAddress;
    private Selector selector;

    private int readLimit;
    private int writeLimit;
    private long timeout;
    
    private LinkedBlockingQueue<Command> commandQueue;
    private LinkedBlockingQueue<IncomingMessageListener> incomingMessageListeners;
    
    
    
    private EventLoop eventLoop;

    private Lock accessLock;

    public TcpTransport(int readLimit, int writeLimit, long connectionTimeout) throws IOException {
        this(null, readLimit, writeLimit, connectionTimeout);
    }

    public TcpTransport(int port, int readLimit, int writeLimit, long connectionTimeout) throws IOException {
        this(new InetSocketAddress(port), readLimit, writeLimit, connectionTimeout);
    }

    public TcpTransport(InetSocketAddress listenAddress, int readLimit, int writeLimit, long timeout) throws IOException {
        //Validate.notNull(listenAddress); // null = no server
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, readLimit);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, writeLimit);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);

        this.listenAddress = listenAddress;
        this.selector = Selector.open();
        
        this.readLimit = readLimit;
        this.writeLimit = writeLimit;
        this.timeout = timeout;
        
        this.commandQueue = new LinkedBlockingQueue<>();
        this.incomingMessageListeners = new LinkedBlockingQueue<>();
        
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
    public void sendMessage(OutgoingMessage message, OutgoingMessageResponseListener listener) {
        Validate.notNull(message);
        Validate.notNull(listener);
        Validate.validState(eventLoop != null && eventLoop.isRunning());
        
        commandQueue.add(new CommandSendRequest(message, listener));
        selector.wakeup();
    }

    @Override
    public void addMessageListener(IncomingMessageListener listener) {
        Validate.notNull(listener);
        Validate.validState(eventLoop != null && eventLoop.isRunning());
        
        incomingMessageListeners.add(listener);
    }

    @Override
    public void removeMessageListener(IncomingMessageListener listener) {
        Validate.notNull(listener);
        Validate.validState(eventLoop != null && eventLoop.isRunning());
        
        incomingMessageListeners.remove(listener);
    }
    
    private final class EventLoop extends AbstractExecutionThreadService {
        
        private ServerSocketChannel serverChannel;
        private Map<SocketChannel, ChannelInfo> channelInfoMap;
        private AtomicBoolean stop;

        public EventLoop() throws IOException {
            try {
                if (listenAddress != null) {
                    serverChannel = ServerSocketChannel.open();
                }
            } catch (RuntimeException | IOException e) {
                IOUtils.closeQuietly(selector);
                IOUtils.closeQuietly(serverChannel);
                throw e;
            }
        }

        @Override
        protected void startUp() throws Exception {
            channelInfoMap = new HashMap<>();
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
            
            commandQueue.clear();
        }

        @Override
        protected void run() {
            ByteBuffer tempBuffer = ByteBuffer.allocate(65535);

            LinkedList<Event> internalEventQueue = new LinkedList<>();
            LinkedList<Command> dumpedCommandQueue = new LinkedList<>();
            while (true) {
                // get current time
                long currentTime = System.currentTimeMillis();

                // dump commands and loop over them
                commandQueue.drainTo(dumpedCommandQueue);

                for (Command command : dumpedCommandQueue) {
                    try {
                        if (command instanceof CommandSendRequest) {
                            createAndInitializeOutgoingSocket(internalEventQueue, (CommandSendRequest) command);
                        } else if (command instanceof CommandSendResponse) {
                            CommandSendResponse commandSr = (CommandSendResponse) command;

                            SocketChannel channel = commandSr.getChannel();
                            OutgoingResponse data = commandSr.getData();
                            ChannelInfo info = channelInfoMap.get(channel);

                            if (info != null) {
                                SelectionKey key = info.getSelectionKey();
                                StreamIoBuffers buffers = info.getBuffers();

                                buffers.startWriting(data.getData());
                                key.interestOps(SelectionKey.OP_WRITE);
                            }
                        } else if (command instanceof CommandKillEstablished) {
                            CommandKillEstablished commandKe = (CommandKillEstablished) command;
                            
                            SocketChannel channel = commandKe.getChannel();
                            ChannelInfo info = channelInfoMap.get(channel);
                            
                            if (info != null) {
                                killSocketSilently(info.getChannel());
                            }
                        } else {
                            throw new IllegalStateException("Unknown command " + command);
                        }
                    } catch (IOException | RuntimeException e) {
                        e.printStackTrace();
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

                // update current time
                currentTime = System.currentTimeMillis();
                
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
                            acceptAndInitializeIncomingSocket(internalEventQueue);
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

                            if (params instanceof OutgoingMessageChannelInfo) {
                                // if this is an outgoing message, first thing we want to do is write
                                key.interestOps(SelectionKey.OP_WRITE);
                            } else {
                                // should never happen
                                throw new IllegalStateException();
                            }
                        } catch (RuntimeException | IOException e) {
                            e.printStackTrace();
                            killSocketDueToError(clientChannel, internalEventQueue, e);
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

                                InetSocketAddress from = (InetSocketAddress) clientChannel.getRemoteAddress();
                                
                                if (params instanceof OutgoingMessageChannelInfo) {
                                    OutgoingMessageResponseListener<InetSocketAddress> receiver =
                                            ((OutgoingMessageChannelInfo) params).getResponseHandler();
                                    IncomingResponse<InetSocketAddress> incomingResponse = new IncomingResponse<>(from, inData,
                                            currentTime);
                                    
                                    EventResponseArrived eventRa = new EventResponseArrived(incomingResponse, receiver);
                                    internalEventQueue.add(eventRa);
                                    
                                    killSocketSilently(clientChannel);
                                } else if (params instanceof IncomingMessageChannelInfo) {
                                    IncomingMessage<InetSocketAddress> incomingMessage = new IncomingMessage<>(from, inData, currentTime);
                                    
                                    EventRequestArrived eventRa = new EventRequestArrived(incomingMessage, selector, clientChannel);
                                    internalEventQueue.add(eventRa);
                                    
                                    key.interestOps(0); // don't need to read anymore, when we get a response, we register op_write
                                } else {
                                    throw new IllegalStateException();
                                }
                            }
                        } catch (RuntimeException | IOException e) {
                            e.printStackTrace();
                            killSocketDueToError(clientChannel, internalEventQueue, e);
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

                                    if (params instanceof OutgoingMessageChannelInfo) {
                                        buffers.startReading();
                                        key.interestOps(SelectionKey.OP_READ);
                                    } else if (params instanceof IncomingMessageChannelInfo) {
                                        killSocketSilently(clientChannel);
                                    } else {
                                        throw new IllegalStateException();
                                    }
                                }
                            }
                        } catch (RuntimeException | IOException e) {
                            e.printStackTrace();
                            killSocketDueToError(clientChannel, internalEventQueue, e);
                        }
                    }
                }

                processEvents(internalEventQueue);
                internalEventQueue.clear();
            }
        }

        private void processEvents(LinkedList<Event> internalEventQueue) {
            IncomingMessageListener[] handlersArray = incomingMessageListeners.toArray(new IncomingMessageListener[0]);
            
            for (Event event : internalEventQueue) {
                if (event instanceof EventRequestArrived) {
                    EventRequestArrived request = (EventRequestArrived) event;

                    for (IncomingMessageListener handler : handlersArray) {
                        IncomingMessage<InetSocketAddress> data = request.getRequest();
                        Selector selector = request.getSelector();
                        SocketChannel channel = request.getChannel();

                        TcpIncomingMessageResponseHandler responseSender = new TcpIncomingMessageResponseHandler(commandQueue, selector, channel);

                        try {
                            handler.messageArrived(data, responseSender);
                        } catch (RuntimeException re) {
                            // kill the socket, don't bother notifying the others
                            killSocketSilently(channel);
                            break;
                        }
                    }
                } else if (event instanceof EventResponseArrived) {
                    EventResponseArrived response = (EventResponseArrived) event;
                    IncomingResponse<InetSocketAddress> data = response.getResponse();

                    try {
                        response.getReceiver().responseArrived(data);
                    } catch (RuntimeException re) {
                        // do nothing
                    }
                } else if (event instanceof EventResponseErrored) {
                    EventResponseErrored response = (EventResponseErrored) event;
                    Throwable error = response.getError();

                    try {
                        response.getReceiver().internalErrorOccurred(error);
                    } catch (RuntimeException re) {
                        // do nothing
                    }
                }
            }
        }
        
        private void killSocketSilently(SocketChannel channel) {
            Validate.notNull(channel);
            
            ChannelInfo info = channelInfoMap.remove(channel);
            if (info != null) {
                SelectionKey key = info.getSelectionKey();
                key.cancel();
            }

            IOUtils.closeQuietly(channel);
        }

        private void killSocketDueToError(SocketChannel channel, LinkedList<Event> internalEventQueue, Throwable error) {
            Validate.notNull(channel);
            Validate.isTrue(!(internalEventQueue == null ^ error == null)); // if one is set but the other isn't, crap out
            
            ChannelInfo info = channelInfoMap.get(channel);
            killSocketSilently(channel);
            
            if (info instanceof OutgoingMessageChannelInfo && error != null) {
                OutgoingMessageResponseListener<InetSocketAddress> receiver = ((OutgoingMessageChannelInfo) info).getResponseHandler();
                EventResponseErrored eventRea = new EventResponseErrored(error, receiver);
                internalEventQueue.add(eventRea);
            }
        }

        private ChannelInfo acceptAndInitializeIncomingSocket(LinkedList<Event> internalEventQueue) throws IOException {
            Validate.notNull(internalEventQueue);
            
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

                ChannelInfo info = new IncomingMessageChannelInfo(clientChannel, buffers, selectionKey);

                channelInfoMap.put(clientChannel, info);

                return info;
            } catch (IOException | RuntimeException e) {
                if (clientChannel != null) {
                    killSocketDueToError(clientChannel, internalEventQueue, e);
                }
                throw e;
            }
        }

        private void createAndInitializeOutgoingSocket(LinkedList<Event> internalEventQueue,
                CommandSendRequest queuedRequest) throws IOException {
            Validate.notNull(queuedRequest);
            Validate.notNull(internalEventQueue);

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

                OutgoingMessage<InetSocketAddress> message = queuedRequest.getMessage();
                InetSocketAddress destinationAddress = message.getTo();

                StreamIoBuffers buffers = new StreamIoBuffers(StreamIoBuffers.Mode.WRITE_FIRST, readLimit, writeLimit);
                buffers.startWriting(message.getData());

                ChannelInfo info = new OutgoingMessageChannelInfo(clientChannel, buffers, selectionKey,
                        queuedRequest.getResponseListener());

                channelInfoMap.put(clientChannel, info);

                clientChannel.connect(destinationAddress);
            } catch (IOException | RuntimeException e) {
                if (clientChannel != null) {
                    killSocketDueToError(clientChannel, internalEventQueue, e);
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
}
