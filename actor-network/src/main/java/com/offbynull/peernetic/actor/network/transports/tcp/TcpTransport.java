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
package com.offbynull.peernetic.actor.network.transports.tcp;

import com.offbynull.peernetic.actor.ActorStartSettings;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PullQueue;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.SelectorActorQueueNotifier;
import com.offbynull.peernetic.actor.helpers.TimeoutManager;
import com.offbynull.peernetic.actor.helpers.TimeoutManager.TimeoutManagerResult;
import com.offbynull.peernetic.actor.network.Deserializer;
import com.offbynull.peernetic.actor.network.IncomingFilter;
import com.offbynull.peernetic.actor.network.NetworkEndpoint;
import com.offbynull.peernetic.actor.network.OutgoingFilter;
import com.offbynull.peernetic.actor.network.Serializer;
import com.offbynull.peernetic.actor.network.Transport;
import com.offbynull.peernetic.actor.network.internal.SendMessageCommand;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

/**
 * A TCP transport implementation.
 * @author Kasra Faghihi
 */
public final class TcpTransport extends Transport<InetSocketAddress> {

    private InetSocketAddress listenAddress;
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private IncomingFilter<InetSocketAddress> incomingFilter;
    private OutgoingFilter<InetSocketAddress> outgoingFilter;
    private Serializer serializer;
    private Deserializer deserializer;

    private Endpoint routeToEndpoint;
    
    private int maxMessageBytes;
    private long timeout;
    
    private long nextId;
    private TimeoutManager<Long> timeoutManager;
    private Map<Long, ChannelInfo> idToChannelInfoMap;

    private ByteBuffer tempBuffer;


    /**
     * Constructs a {@link TcpTransport} object.
     * @param listenAddress listen address of this transport
     * @param maxMessageBytes maximum number of bytes a message can be
     * @param timeout maximum amount of time a socket can exist while idle (either connecting or connected)
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if any numeric arguments are negative
     */
    public TcpTransport(InetSocketAddress listenAddress, int maxMessageBytes, long timeout) {
        Validate.notNull(listenAddress);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxMessageBytes);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, timeout);

        this.listenAddress = listenAddress;
        this.maxMessageBytes = maxMessageBytes;
        this.timeout = timeout;
    }

    @Override
    protected ActorStartSettings onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {
        outgoingFilter = (OutgoingFilter<InetSocketAddress>) initVars.get(OUTGOING_FILTER_KEY);
        incomingFilter = (IncomingFilter<InetSocketAddress>) initVars.get(INCOMING_FILTER_KEY);
        routeToEndpoint = (Endpoint) initVars.get(ENDPOINT_ROUTE_KEY);
        serializer = (Serializer) initVars.get(SERIALIZER_KEY);
        deserializer = (Deserializer) initVars.get(DESERIALIZER_KEY);
        
        tempBuffer = ByteBuffer.allocate(65535);
        
        idToChannelInfoMap = new HashMap<>();
        timeoutManager = new TimeoutManager<>();
        
        try {
            selector = Selector.open();
            
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
        
        return new ActorStartSettings(new SelectorActorQueueNotifier(selector));
    }

    @Override
    protected long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue, Endpoint selfEndpoint) throws Exception {
        // create new socket for each outgoing msg
        Incoming incoming;
        while ((incoming = pullQueue.pull()) != null) {
            Object content = incoming.getContent();

            if (content instanceof SendMessageCommand) {
                SendMessageCommand<InetSocketAddress> smc = (SendMessageCommand) content;
                createAndInitializeOutgoingSocket(timestamp, smc.getDestination(), smc.getContent());
            } else {
                throw new IllegalStateException();
            }
        }
        
        
        // get timed out channels and clear them + get max amount of time to wait till next timeout
        TimeoutManagerResult<Long> timeoutRes = timeoutManager.process(timestamp);
        long waitDuration = timeoutRes.getNextTimeoutTimestamp();
        for (Long id : timeoutRes.getTimedout()) {
            killSocketSilently(id);
        }


        
        // reselect
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
                    acceptAndInitializeIncomingSocket(timestamp);
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
                        key.interestOps(SelectionKey.OP_WRITE);
                    } else {
                        throw new IllegalStateException();
                    }
                } catch (RuntimeException | IOException e) {
                    System.out.println("KILLED SILENTLY err5");
                    killSocketSilently(id);
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
                        ByteBuffer inData = ByteBuffer.wrap(buffers.finishReading());

                        if (info instanceof OutgoingMessageChannelInfo) {
                            // don't care what we got back, but it should be byte[] { 0x00 }.
                            killSocketSilently(id);
                        } else if (info instanceof IncomingMessageChannelInfo) {
                            int returnPort = inData.getShort() & 0xFFFF;

                            InetSocketAddress socketFrom = (InetSocketAddress) clientChannel.getRemoteAddress();
                            InetSocketAddress from = new InetSocketAddress(socketFrom.getAddress(), returnPort);

                            ByteBuffer filteredInData = incomingFilter.filter(from, inData);
                            Object content = deserializer.deserialize(filteredInData);
                            
                            NetworkEndpoint<InetSocketAddress> networkEndpoint = new NetworkEndpoint(selfEndpoint, from);
                            pushQueue.push(networkEndpoint, routeToEndpoint, content);

                            info.getBuffers().startWriting(new byte[] {0x00}); // write a marker saying you got it.
                            key.interestOps(SelectionKey.OP_WRITE);
                        } else {
                            throw new IllegalStateException();
                        }
                    }
                } catch (RuntimeException | IOException e) {
                    e.printStackTrace();
                    killSocketSilently(id);
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
                    System.out.println("KILLED SILENTLY err3");
                    killSocketSilently(id);
                }
            }
        }

        return waitDuration;
    }

    @Override
    protected void onStop(long timestamp, PushQueue pushQueue) throws Exception {
        Set<Long> ids = new HashSet<>(idToChannelInfoMap.keySet());
        for (Long id : ids) {
            killSocketSilently(id);
        }
        
        IOUtils.closeQuietly(serverChannel);
        IOUtils.closeQuietly(selector);
    }
    
    private ChannelInfo acceptAndInitializeIncomingSocket(long timestamp) throws IOException {
        SocketChannel clientChannel = null;
        SelectionKey selectionKey;

        long id = nextId++;
        
        try {
            clientChannel = serverChannel.accept();
            clientChannel.configureBlocking(false);
            clientChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
            clientChannel.setOption(StandardSocketOptions.SO_LINGER, 0);
            clientChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);

            selectionKey = clientChannel.register(selector, SelectionKey.OP_READ); // no need to OP_CONNECT
            selectionKey.attach(id);
            StreamIoBuffers buffers = new StreamIoBuffers(StreamIoBuffers.Mode.READ_FIRST, maxMessageBytes, 1);
            buffers.startReading();

            ChannelInfo info = new IncomingMessageChannelInfo(clientChannel, buffers, selectionKey);

            idToChannelInfoMap.put(id, info);
            timeoutManager.add(id, timestamp + timeout);

            return info;
        } catch (IOException | RuntimeException e) {
            System.out.println("KILLED SILENTLY err2");
            if (clientChannel != null) {
                killSocketSilently(id);
            }
            
            return null;
        }
    }
    
    private ChannelInfo createAndInitializeOutgoingSocket(long timestamp, InetSocketAddress to, Object content)
            throws IOException {
        Validate.notNull(to);
        Validate.notNull(content);

        SocketChannel clientChannel = null;
        SelectionKey selectionKey;

        long id = nextId++;
        
        try {
            clientChannel = SocketChannel.open();

            clientChannel.configureBlocking(false);
            clientChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
            clientChannel.setOption(StandardSocketOptions.SO_LINGER, 0);
            clientChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            
            selectionKey = clientChannel.register(selector, SelectionKey.OP_CONNECT);
            selectionKey.attach(id);

            StreamIoBuffers buffers = new StreamIoBuffers(StreamIoBuffers.Mode.WRITE_FIRST, 1, maxMessageBytes);

            ByteBuffer data = serializer.serialize(content);
            ByteBuffer filteredOutData = outgoingFilter.filter(to, data);
            
            int selfPort = listenAddress.getPort();
            
            ByteBuffer finalData = ByteBuffer.allocate(2 + filteredOutData.remaining());
            finalData.putShort((short) (selfPort & 0xFFFF));
            finalData.put(filteredOutData);
            finalData.flip();

            buffers.startWriting(finalData);

            ChannelInfo info = new OutgoingMessageChannelInfo(clientChannel, buffers, selectionKey);

            idToChannelInfoMap.put(id, info);
            timeoutManager.add(id, timestamp + timeout);

            clientChannel.connect(to);

            return info;
        } catch (IOException | RuntimeException e) {
            System.out.println("KILLED SILENTLY err1");
            if (clientChannel != null) {
                killSocketSilently(id);
            }

            return null;
        }
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
}
