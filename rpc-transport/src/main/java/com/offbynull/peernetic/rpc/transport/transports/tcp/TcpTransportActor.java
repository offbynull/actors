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

import com.offbynull.peernetic.common.concurrent.actor.ActorQueue;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.common.concurrent.actor.PushQueue;
import com.offbynull.peernetic.common.concurrent.actor.SelectorActorQueueNotifier;
import com.offbynull.peernetic.common.concurrent.actor.helpers.TimeoutManager;
import com.offbynull.peernetic.common.concurrent.actor.helpers.TimeoutManager.TimeoutManagerResult;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import com.offbynull.peernetic.rpc.transport.OutgoingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.internal.DefaultIncomingResponseListener;
import com.offbynull.peernetic.rpc.transport.internal.DropResponseCommand;
import com.offbynull.peernetic.rpc.transport.internal.SendRequestCommand;
import com.offbynull.peernetic.rpc.transport.internal.TransportActor;
import com.offbynull.peernetic.rpc.transport.internal.SendResponseCommand;
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
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

final class TcpTransportActor extends TransportActor<InetSocketAddress> {

    private InetSocketAddress listenAddress;
    private Selector selector;

    private IncomingFilter<InetSocketAddress> incomingFilter;
    private OutgoingFilter<InetSocketAddress> outgoingFilter;
    
    private IncomingMessageListener<InetSocketAddress> incomingMessageListener;
    
    private int readLimit;
    private int writeLimit;
    
    private long timeout;
    
    private ServerSocketChannel serverChannel;

    private long nextId;
    private TimeoutManager<Long> timeoutManager;
    private Map<Long, ChannelInfo> idToChannelInfoMap;
    
    private ByteBuffer tempBuffer = ByteBuffer.allocate(65535);

    public TcpTransportActor(InetSocketAddress listenAddress, int readLimit, int writeLimit, long timeout) throws IOException {
        super(true);
        
        Validate.notNull(listenAddress);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, readLimit);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, writeLimit);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);

        this.listenAddress = listenAddress;

        this.readLimit = readLimit;
        this.writeLimit = writeLimit;
        this.timeout = timeout;
    }

    @Override
    protected ActorQueue createQueue() {
        try {
            this.selector = Selector.open();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return new ActorQueue(new SelectorActorQueueNotifier(selector));
    }

    @Override
    protected void onStart() throws Exception {
        idToChannelInfoMap = new HashMap<>();

        incomingMessageListener = getIncomingMessageListener();

        incomingFilter = getIncomingFilter();
        outgoingFilter = getOutgoingFilter();
        
        timeoutManager = new TimeoutManager<>();
        
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            serverChannel.socket().bind(listenAddress);
        } catch (RuntimeException | IOException e) {
            IOUtils.closeQuietly(selector);
            IOUtils.closeQuietly(serverChannel);
            throw e;
        }
    }

    @Override
    protected long onStep(long timestamp, Iterator<Message> iterator, PushQueue pushQueue) throws Exception {
        // process commands
        while (iterator.hasNext()) {
            Message msg = iterator.next();
            Object content = msg.getContent();
            
            if (content instanceof SendRequestCommand) {
                SendRequestCommand src = (SendRequestCommand) content;
                
                long newId = nextId++;
                createAndInitializeOutgoingSocket(timestamp, newId, src);
            } else if (content instanceof SendResponseCommand) {
                SendResponseCommand src = (SendResponseCommand) content;
                
                Long responseToId = msg.getResponseToId(Long.class);
                if (responseToId == null) {
                    continue;
                }

                ChannelInfo info = idToChannelInfoMap.get(responseToId);
                if (info == null) {
                    continue;
                }

                SelectionKey key = info.getSelectionKey();
                StreamIoBuffers buffers = info.getBuffers();

                SocketChannel channel = info.getChannel();
                ByteBuffer filteredOutData = outgoingFilter.filter(
                        (InetSocketAddress) channel.getRemoteAddress(), src.getData());

                buffers.startWriting(filteredOutData);
                key.interestOps(SelectionKey.OP_WRITE);
            } else if (content instanceof DropResponseCommand) {
                Long responseToId = msg.getResponseToId(Long.class);
                if (responseToId == null) {
                    continue;
                }

                ChannelInfo info = idToChannelInfoMap.get(responseToId);
                if (info == null) {
                    continue;
                }
                
                killSocketSilently(responseToId);
            } else {
                throw new IllegalArgumentException();
            }
        }



        // get timed out channels + max amount of time to wait till next timeout
        TimeoutManagerResult<Long> timeoutRes = timeoutManager.process(timestamp);
        long waitDuration = timeoutRes.getNextTimeoutTimestamp();

        // go through timedout connections and notify each of timeout
        for (Long id : timeoutRes.getTimedout()) {
            killSocketDueToError(id, "TimedOut");
        }


        selector.selectNow();
        
        
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
                    long newId = nextId++;
                    
                    acceptAndInitializeIncomingSocket(timestamp, newId);
                } catch (RuntimeException | IOException e) {  // NOPMD
                    // do nothing
                }
            } else if (key.isConnectable()) {
                SocketChannel clientChannel = (SocketChannel) key.channel();
                long id = (Long) key.attachment();

                try {
                    ChannelInfo info = idToChannelInfoMap.get(id);

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
                    killSocketDueToError(id, e);
                }
            } else if (key.isReadable()) {
                SocketChannel clientChannel = (SocketChannel) key.channel();
                long id = (Long) key.attachment();

                try {
                    ChannelInfo info = idToChannelInfoMap.get(id);
                    StreamIoBuffers buffers = info.getBuffers();

                    tempBuffer.clear();
                    if (clientChannel.read(tempBuffer) != -1) {
                        buffers.addReadBlock(tempBuffer);
                    } else {
                        clientChannel.shutdownInput();
                        byte[] inData = buffers.finishReading();
                        InetSocketAddress from = (InetSocketAddress) clientChannel.getRemoteAddress();

                        ByteBuffer filteredInData = incomingFilter.filter(from, ByteBuffer.wrap(inData));

                        if (info instanceof OutgoingMessageChannelInfo) {
                            OutgoingMessageResponseListener listener = ((OutgoingMessageChannelInfo) info).getResponseHandler();
                            try {
                                listener.responseArrived(filteredInData);
                            } catch (RuntimeException re) { // NOPMD
                            }

                            killSocketSilently(id);
                        } else if (info instanceof IncomingMessageChannelInfo) {
                            IncomingMessageResponseListener responseCallback = new DefaultIncomingResponseListener(id, getSelfWriter());
                            try {
                                incomingMessageListener.messageArrived(from, filteredInData, responseCallback);
                            } catch (RuntimeException re) { // NOPMD
                            }

                            key.interestOps(0); // don't need to read anymore, when we get a response, we register op_write
                        } else {
                            throw new IllegalStateException();
                        }
                    }
                } catch (RuntimeException | IOException e) {
                    killSocketDueToError(id, e);
                }
            } else if (key.isWritable()) {
                SocketChannel clientChannel = (SocketChannel) key.channel();
                long id = (Long) key.attachment();

                try {
                    ChannelInfo info = idToChannelInfoMap.get(id);
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
                                killSocketSilently(id);
                            } else {
                                throw new IllegalStateException();
                            }
                        }
                    }
                } catch (RuntimeException | IOException e) {
                    killSocketDueToError(id, e);
                }
            }
        }

        return waitDuration;
    }

    private void killSocketSilently(long id) {
        ChannelInfo info = idToChannelInfoMap.remove(id);
        timeoutManager.cancel(id);
        if (info != null) {
            SelectionKey key = info.getSelectionKey();
            key.cancel();
            IOUtils.closeQuietly(info.getChannel());
        }
    }

    private void killSocketDueToError(long id, Object error) {
        ChannelInfo info = idToChannelInfoMap.get(id);
        if (info != null) {
            killSocketSilently(id);
            if (info instanceof OutgoingMessageChannelInfo) {
                OutgoingMessageResponseListener listener = ((OutgoingMessageChannelInfo) info).getResponseHandler();
                try {
                    listener.errorOccurred(error);
                } catch (RuntimeException re) { //NOPMD
                }
            }
        } else {
            timeoutManager.cancel(id); // needs to be called again if info == null, in case this was added but the idToChannelInfoMap
                                       // entry wasn't
        }
    }

    private ChannelInfo acceptAndInitializeIncomingSocket(long timestamp, long newId) throws IOException {
        SocketChannel clientChannel = null;
        SelectionKey selectionKey;

        try {
            clientChannel = serverChannel.accept();
            clientChannel.configureBlocking(false);
            clientChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
            clientChannel.setOption(StandardSocketOptions.SO_LINGER, 0);
            clientChannel.setOption(StandardSocketOptions.TCP_NODELAY, false);

            selectionKey = clientChannel.register(selector, SelectionKey.OP_READ); // no need to OP_CONNECT
            selectionKey.attach(newId);
            StreamIoBuffers buffers = new StreamIoBuffers(StreamIoBuffers.Mode.READ_FIRST, readLimit, writeLimit);
            buffers.startReading();

            ChannelInfo info = new IncomingMessageChannelInfo(clientChannel, buffers, selectionKey);

            idToChannelInfoMap.put(newId, info);
            timeoutManager.add(newId, timestamp + timeout);

            return info;
        } catch (IOException | RuntimeException e) {
            if (clientChannel != null) {
                killSocketSilently(newId);
            }
            
            return null;
        }
    }

    private ChannelInfo createAndInitializeOutgoingSocket(long timestamp, long newId, SendRequestCommand<InetSocketAddress> queuedRequest)
            throws IOException {
        Validate.notNull(queuedRequest);

        SocketChannel clientChannel = null;
        SelectionKey selectionKey;

        try {
            clientChannel = SocketChannel.open();

            clientChannel.configureBlocking(false);
            clientChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
            clientChannel.setOption(StandardSocketOptions.SO_LINGER, 0);
            clientChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);

            selectionKey = clientChannel.register(selector, SelectionKey.OP_CONNECT);
            selectionKey.attach(newId);

            ByteBuffer data = queuedRequest.getData();
            InetSocketAddress dst = queuedRequest.getTo();

            StreamIoBuffers buffers = new StreamIoBuffers(StreamIoBuffers.Mode.WRITE_FIRST, readLimit, writeLimit);

            ByteBuffer filteredOutData = outgoingFilter.filter(dst, data);

            buffers.startWriting(filteredOutData);

            ChannelInfo info = new OutgoingMessageChannelInfo(clientChannel, buffers, selectionKey, queuedRequest.getListener());

            idToChannelInfoMap.put(newId, info);
            timeoutManager.add(newId, timestamp + timeout);

            clientChannel.connect(dst);

            return info;
        } catch (IOException | RuntimeException e) {
            if (clientChannel != null) {
                killSocketDueToError(newId, e);
            }

            return null;
        }
    }

    @Override
    protected void onStop(PushQueue pushQueue) throws Exception {
        IOUtils.closeQuietly(selector);
        IOUtils.closeQuietly(serverChannel);

        for (Entry<Long, ChannelInfo> e : idToChannelInfoMap.entrySet()) {
            killSocketDueToError(e.getKey(), "Shutdown");
        }
    }
}