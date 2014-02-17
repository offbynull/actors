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
package com.offbynull.peernetic.router.common;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.offbynull.peernetic.common.utils.ByteBufferUtils;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Executor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class UdpCommunicator extends AbstractExecutionThreadService {
    private final LinkedBlockingQueue<UdpCommunicatorListener> listeners;
    private final Map<DatagramChannel, LinkedBlockingQueue<ImmutablePair<InetSocketAddress, ByteBuffer>>> sendQueue;
    private volatile boolean stopFlag;
    private volatile Selector selector;

    public UdpCommunicator(List<DatagramChannel> channels) {
        Validate.noNullElements(channels);
        
        listeners = new LinkedBlockingQueue<>();
        
        Map<DatagramChannel, LinkedBlockingQueue<ImmutablePair<InetSocketAddress, ByteBuffer>>> intSendQueue = new HashMap<>();
        for (DatagramChannel channel :  channels) {
            intSendQueue.put(channel, new LinkedBlockingQueue<ImmutablePair<InetSocketAddress, ByteBuffer>>());
        }
        sendQueue = Collections.unmodifiableMap(intSendQueue);
    }

    public void addListener(UdpCommunicatorListener e) {
        if (!isRunning()) {
            throw new IllegalStateException();
        }
        listeners.add(e);
    }

    public void removeListener(UdpCommunicatorListener e) {
        if (!isRunning()) {
            throw new IllegalStateException();
        }
        listeners.remove(e);
    }
    
    public void send(DatagramChannel channel, InetSocketAddress dst, ByteBuffer data) {
        if (!isRunning()) {
            throw new IllegalStateException();
        }
        LinkedBlockingQueue<ImmutablePair<InetSocketAddress, ByteBuffer>> queue = sendQueue.get(channel);
        queue.add(new ImmutablePair<>(dst, ByteBufferUtils.copyContents(data)));
        
        selector.wakeup();
    }
    
    @Override
    protected void startUp() throws Exception {
        selector = Selector.open();
        
        for (DatagramChannel channel : sendQueue.keySet()) {
            channel.register(selector, SelectionKey.OP_READ);
        }
    }

    @Override
    protected void run() throws Exception {
        ByteBuffer recvBuffer = ByteBuffer.allocate(1100);
        
        while (true) {
            selector.select();
            if (stopFlag) {
                return;
            }
            
            for (DatagramChannel channel : sendQueue.keySet()) {
                if (!sendQueue.get(channel).isEmpty()) {
                    channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                } else {
                    channel.register(selector, SelectionKey.OP_READ);
                }
            }
            
            for (SelectionKey key : selector.selectedKeys()) {
                if (!key.isValid()) {
                    continue;
                }
                
                DatagramChannel channel = (DatagramChannel) key.channel();
                
                if (key.isReadable()) {
                    recvBuffer.clear();
                    InetSocketAddress incomingAddress = (InetSocketAddress) channel.receive(recvBuffer);
                    recvBuffer.flip();
                    for (UdpCommunicatorListener listener : listeners) {
                        try {
                            listener.incomingPacket(incomingAddress, channel, recvBuffer.asReadOnlyBuffer());
                        } catch (RuntimeException re) { // NOPMD
                            // do nothing
                        }
                    }
                } else if (key.isWritable()) {
                    LinkedBlockingQueue<ImmutablePair<InetSocketAddress, ByteBuffer>> queue = sendQueue.get(channel);
                    ImmutablePair<InetSocketAddress, ByteBuffer> next = queue.poll();
                    
                    if (next != null) {
                        channel.send(next.getValue(), next.getKey());
                    }
                }
            }
        }
    }

    @Override
    protected void shutDown() throws Exception {
        IOUtils.closeQuietly(selector);
        for (DatagramChannel channel : sendQueue.keySet()) {
            IOUtils.closeQuietly(channel);
        }
    }
    
    @Override
    protected void triggerShutdown() {
        stopFlag = true;
        selector.wakeup();
    }

    @Override
    protected Executor executor() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                Thread thread = new Thread(command);
                thread.setDaemon(true);
                thread.start();
            }
        };
    }

    @Override
    protected String serviceName() {
        return getClass().getSimpleName();
    }
}
