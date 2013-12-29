/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.rpc.transport.transports.tcp;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingMessage;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.IncomingResponse;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import com.offbynull.peernetic.rpc.transport.OutgoingMessage;
import com.offbynull.peernetic.rpc.transport.OutgoingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.OutgoingResponse;
import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transport.transports.tcp.TimeoutManager.Result;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
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

/**
 * A TCP transport implementation.
 * @author Kasra Faghihi
 */
public final class TcpTransport implements Transport<InetSocketAddress> {

    private InetSocketAddress listenAddress;
    private Selector selector;

    private int readLimit;
    private int writeLimit;
    private long timeout;
    
    private LinkedBlockingQueue<Command> commandQueue;
    
    
    
    private EventLoop eventLoop;

    private Lock accessLock;


    /**
     * Constructs a {@link TcpTransport} object.
     * @param port port to listen on
     * @param readLimit read limit
     * @param writeLimit write limit
     * @param timeout timeout duration
     * @throws IOException on error
     * @throws IllegalArgumentException if port is out of range, or if any of the other arguments are {@code <= 0};
     */
    public TcpTransport(int port, int readLimit, int writeLimit, long timeout) throws IOException {
        this(new InetSocketAddress(port), readLimit, writeLimit, timeout);
    }

    /**
     * Constructs a {@link TcpTransport} object.
     * @param listenAddress address to listen on
     * @param readLimit read limit
     * @param writeLimit write limit
     * @param timeout timeout duration
     * @throws IOException on error
     * @throws IllegalArgumentException if port is out of range, or if any of the other arguments are {@code <= 0};
     */
    public TcpTransport(InetSocketAddress listenAddress, int readLimit, int writeLimit, long timeout) throws IOException {
        Validate.notNull(listenAddress);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, readLimit);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, writeLimit);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);

        this.listenAddress = listenAddress;
        this.selector = Selector.open();
        
        this.readLimit = readLimit;
        this.writeLimit = writeLimit;
        this.timeout = timeout;
        
        this.commandQueue = new LinkedBlockingQueue<>();
        
        accessLock = new ReentrantLock();
    }

    @Override
    public void start(IncomingFilter<InetSocketAddress> incomingFilter, IncomingMessageListener<InetSocketAddress> incomingMessageListener,
            OutgoingFilter<InetSocketAddress> outgoingFilter) throws IOException {
        accessLock.lock();
        try {
            if (eventLoop != null) {
                throw new IllegalStateException();
            }

            Validate.notNull(incomingFilter);
            Validate.notNull(outgoingFilter);
            Validate.notNull(incomingMessageListener);

            eventLoop = new EventLoop(incomingFilter, incomingMessageListener, outgoingFilter);
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
    
    private final class EventLoop extends AbstractExecutionThreadService {
        
        private ServerSocketChannel serverChannel;
        private Map<SocketChannel, ChannelInfo> channelInfoMap;
        private AtomicBoolean stop;
        
        private TimeoutManager timeoutManager;
        
        private IncomingFilter<InetSocketAddress> inFilter;
        private IncomingMessageListener<InetSocketAddress> handler;
        private OutgoingFilter<InetSocketAddress> outFilter;

        public EventLoop(IncomingFilter<InetSocketAddress> incomingFilter,
                IncomingMessageListener<InetSocketAddress> incomingMessageListener,
                OutgoingFilter<InetSocketAddress> outgoingFilter) throws IOException {
            this.inFilter = incomingFilter;
            this.handler = incomingMessageListener;
            this.outFilter = outgoingFilter;
            timeoutManager = new TimeoutManager(timeout);
            
            try {
                serverChannel = ServerSocketChannel.open();
                serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
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
                serverChannel.configureBlocking(false);
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);
                serverChannel.socket().bind(listenAddress);
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
                            ChannelInfo info = createAndInitializeOutgoingSocket(internalEventQueue, (CommandSendRequest) command);
                            SocketChannel clientChannel = info.getChannel();
                            timeoutManager.addChannel(clientChannel, currentTime);
                        } else if (command instanceof CommandSendResponse) {
                            CommandSendResponse commandSr = (CommandSendResponse) command;

                            SocketChannel channel = commandSr.getChannel();
                            OutgoingResponse data = commandSr.getData();
                            ChannelInfo info = channelInfoMap.get(channel);

                            if (info != null) {
                                SelectionKey key = info.getSelectionKey();
                                StreamIoBuffers buffers = info.getBuffers();
                                
                                ByteBuffer filteredOutData = outFilter.filter(
                                        (InetSocketAddress) channel.getRemoteAddress(), data.getData());

                                buffers.startWriting(filteredOutData);
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
                    } catch (IOException | RuntimeException e) { // NOPMD
                        // do nothing
                    }
                }
                dumpedCommandQueue.clear();

                // get timed out channels + max amount of time to wait till next timeout
                Result timeoutRes = timeoutManager.evaluate(currentTime);
                long waitDuration = timeoutRes.getWaitDuration();

                // go through timedout connections and add timeout events for each of them + kill them
                boolean timeoutEventAdded = false;
                for (SocketChannel channel : timeoutRes.getTimedOutChannels()) {
                    ChannelInfo info = channelInfoMap.get(channel);
                    
                    killSocketSilently(channel);
                    
                    // if it was an outgoing message, we need to inform the response handler that the connection's dead
                    if (info == null || !(info instanceof OutgoingMessageChannelInfo)) {
                        continue;
                    }
                    
                    internalEventQueue.add(new EventResponseTimedOut(((OutgoingMessageChannelInfo) info).getResponseHandler()));
                    timeoutEventAdded = true;
                }
                
                // select
                try {
                    // if timeout event was added then don't wait, because we need to process those events
                    if (timeoutEventAdded) {
                        selector.selectNow();
                    } else {
                        selector.select(waitDuration);
                    }
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
                            ChannelInfo info = acceptAndInitializeIncomingSocket(internalEventQueue);
                            SocketChannel clientChannel = info.getChannel();
                            timeoutManager.addChannel(clientChannel, currentTime);
                        } catch (RuntimeException | IOException e) {  // NOPMD
                            // do nothing
                        }
                    } else if (key.isConnectable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();

                        try {
                            ChannelInfo info = channelInfoMap.get(clientChannel);

                            if (!clientChannel.finishConnect()) {
                                throw new RuntimeException();
                            }

                            if (info instanceof OutgoingMessageChannelInfo) {
                                // if this is an outgoing message, first thing we want to do is write
                                key.interestOps(SelectionKey.OP_WRITE);
                            } else {
                                // should never happen
                                throw new IllegalStateException();
                            }
                        } catch (RuntimeException | IOException e) {
                            killSocketDueToError(clientChannel, internalEventQueue, e);
                        }
                    } else if (key.isReadable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();

                        try {
                            ChannelInfo info = channelInfoMap.get(clientChannel);
                            StreamIoBuffers buffers = info.getBuffers();

                            tempBuffer.clear();
                            if (clientChannel.read(tempBuffer) != -1) {
                                buffers.addReadBlock(tempBuffer);
                            } else {
                                clientChannel.shutdownInput();
                                byte[] inData = buffers.finishReading();
                                InetSocketAddress from = (InetSocketAddress) clientChannel.getRemoteAddress();
                                
                                ByteBuffer filteredInData = inFilter.filter(from, ByteBuffer.wrap(inData));
                                
                                if (info instanceof OutgoingMessageChannelInfo) {
                                    OutgoingMessageResponseListener<InetSocketAddress> receiver =
                                            ((OutgoingMessageChannelInfo) info).getResponseHandler();
                                    IncomingResponse<InetSocketAddress> incomingResponse = new IncomingResponse<>(from, filteredInData,
                                            currentTime);
                                    
                                    EventResponseArrived eventRa = new EventResponseArrived(incomingResponse, receiver);
                                    internalEventQueue.add(eventRa);
                                    
                                    killSocketSilently(clientChannel);
                                } else if (info instanceof IncomingMessageChannelInfo) {
                                    IncomingMessage<InetSocketAddress> incomingMessage = new IncomingMessage<>(from, filteredInData,
                                            currentTime);
                                    
                                    EventRequestArrived eventRa = new EventRequestArrived(incomingMessage, selector, clientChannel);
                                    internalEventQueue.add(eventRa);
                                    
                                    key.interestOps(0); // don't need to read anymore, when we get a response, we register op_write
                                } else {
                                    throw new IllegalStateException();
                                }
                            }
                        } catch (RuntimeException | IOException e) {
                            killSocketDueToError(clientChannel, internalEventQueue, e);
                        }
                    } else if (key.isWritable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();

                        try {
                            ChannelInfo info = channelInfoMap.get(clientChannel);
                            StreamIoBuffers buffers = info.getBuffers();

                            tempBuffer.clear();
                            buffers.getWriteBlock(tempBuffer);

                            if (tempBuffer.hasRemaining()) {
                                int amountWritten = clientChannel.write(tempBuffer);
                                buffers.adjustWritePointer(amountWritten);

                                if (buffers.isEndOfWrite()) {
                                    clientChannel.shutdownOutput();
                                    buffers.finishWriting();

                                    if (info instanceof OutgoingMessageChannelInfo) {
                                        buffers.startReading();
                                        key.interestOps(SelectionKey.OP_READ);
                                    } else if (info instanceof IncomingMessageChannelInfo) {
                                        killSocketSilently(clientChannel);
                                    } else {
                                        throw new IllegalStateException();
                                    }
                                }
                            }
                        } catch (RuntimeException | IOException e) {
                            killSocketDueToError(clientChannel, internalEventQueue, e);
                        }
                    }
                }

                processEvents(internalEventQueue);
                internalEventQueue.clear();
            }
        }

        private void processEvents(LinkedList<Event> internalEventQueue) {
            for (Event event : internalEventQueue) {
                if (event instanceof EventRequestArrived) {
                    EventRequestArrived request = (EventRequestArrived) event;

                    IncomingMessage<InetSocketAddress> data = request.getRequest();
                    Selector selector = request.getSelector();
                    SocketChannel channel = request.getChannel();

                    TcpIncomingMessageResponseHandler responseSender = new TcpIncomingMessageResponseHandler(commandQueue, selector,
                            channel);

                    try {
                        handler.messageArrived(data, responseSender);
                    } catch (RuntimeException re) {
                        // kill the socket, don't bother notifying the others
                        killSocketSilently(channel);
                        break;
                    }
                } else if (event instanceof EventResponseArrived) {
                    EventResponseArrived response = (EventResponseArrived) event;
                    IncomingResponse<InetSocketAddress> data = response.getResponse();

                    try {
                        response.getReceiver().responseArrived(data);
                    } catch (RuntimeException re) { // NOPMD
                        // do nothing
                    }
                } else if (event instanceof EventResponseErrored) {
                    EventResponseErrored response = (EventResponseErrored) event;
                    Throwable error = response.getError();

                    try {
                        response.getReceiver().internalErrorOccurred(error);
                    } catch (RuntimeException re) { // NOPMD
                        // do nothing
                    }
                } else if (event instanceof EventResponseTimedOut) {
                    EventResponseTimedOut response = (EventResponseTimedOut) event;

                    try {
                        response.getReceiver().timedOut();
                    } catch (RuntimeException re) { // NOPMD
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
                clientChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
                clientChannel.setOption(StandardSocketOptions.SO_LINGER, 0);
                clientChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);

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

        private ChannelInfo createAndInitializeOutgoingSocket(LinkedList<Event> internalEventQueue, CommandSendRequest queuedRequest)
                throws IOException {
            Validate.notNull(queuedRequest);
            Validate.notNull(internalEventQueue);

            SocketChannel clientChannel = null;
            SelectionKey selectionKey;

            try {
                clientChannel = SocketChannel.open();

                clientChannel.configureBlocking(false);
                clientChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
                clientChannel.setOption(StandardSocketOptions.SO_LINGER, 0);
                clientChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);

                selectionKey = clientChannel.register(selector, SelectionKey.OP_CONNECT);

                OutgoingMessage<InetSocketAddress> message = queuedRequest.getMessage();
                InetSocketAddress destinationAddress = message.getTo();

                StreamIoBuffers buffers = new StreamIoBuffers(StreamIoBuffers.Mode.WRITE_FIRST, readLimit, writeLimit);
                
                ByteBuffer filteredOutData = outFilter.filter(destinationAddress, message.getData());

                buffers.startWriting(filteredOutData);

                ChannelInfo info = new OutgoingMessageChannelInfo(clientChannel, buffers, selectionKey,
                        queuedRequest.getResponseListener());

                channelInfoMap.put(clientChannel, info);

                clientChannel.connect(destinationAddress);
                
                return info;
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
                
                ChannelInfo info = e.getValue();
                
                if (info instanceof OutgoingMessageChannelInfo) {
                    ((OutgoingMessageChannelInfo) info).getResponseHandler().internalErrorOccurred(null);
                }
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
