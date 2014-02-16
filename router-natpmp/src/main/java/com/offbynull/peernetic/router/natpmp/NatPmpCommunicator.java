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
package com.offbynull.peernetic.router.natpmp;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.offbynull.peernetic.common.utils.ByteBufferUtils;
import com.offbynull.peernetic.router.common.NetworkUtils;
import com.offbynull.peernetic.router.common.CommunicationType;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

final class NatPmpCommunicator extends AbstractExecutionThreadService {
    private final CopyOnWriteArrayList<NatPmpCommunicatorListener> listeners;
    private final LinkedBlockingQueue<ByteBuffer> sendQueue;
    private volatile boolean stopFlag;
    private volatile Selector selector;
    private DatagramChannel unicastChannel;
    private DatagramChannel ipv4MulticastChannel;
    private DatagramChannel ipv6MulticastChannel;
    private InetAddress gatewayAddress;

    public NatPmpCommunicator(InetAddress gatewayAddress) {
        Validate.notNull(gatewayAddress);
        
        this.gatewayAddress = gatewayAddress;
        listeners = new CopyOnWriteArrayList<>();
        sendQueue = new LinkedBlockingQueue<>();
    }

    public void addListener(NatPmpCommunicatorListener e) {
        if (!isRunning()) {
            throw new IllegalStateException();
        }
        listeners.add(0, e);
    }

    public void removeListener(NatPmpCommunicatorListener e) {
        if (!isRunning()) {
            throw new IllegalStateException();
        }
        listeners.remove(e);
    }
    
    public void send(ByteBuffer data) {
        if (!isRunning()) {
            throw new IllegalStateException();
        }
        sendQueue.add(ByteBufferUtils.copyContents(data));
        selector.wakeup();
    }
    
    @Override
    protected void startUp() throws Exception {
        selector = Selector.open();
        
        unicastChannel = DatagramChannel.open();
        unicastChannel.configureBlocking(false);
        unicastChannel.socket().bind(new InetSocketAddress(0));
        unicastChannel.register(selector, SelectionKey.OP_READ);
        
        ipv4MulticastChannel = DatagramChannel.open(StandardProtocolFamily.INET);
        ipv4MulticastChannel.configureBlocking(false);
        ipv4MulticastChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        ipv4MulticastChannel.socket().bind(new InetSocketAddress(5350));
        ipv4MulticastChannel.register(selector, SelectionKey.OP_READ);
        NetworkUtils.multicastListenOnAllIpv4InterfaceAddresses(ipv4MulticastChannel);
        
        ipv6MulticastChannel = DatagramChannel.open(StandardProtocolFamily.INET6);
        ipv6MulticastChannel.configureBlocking(false);
        ipv6MulticastChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        ipv6MulticastChannel.socket().bind(new InetSocketAddress(5350));
        ipv6MulticastChannel.register(selector, SelectionKey.OP_READ);
        NetworkUtils.multicastListenOnAllIpv6InterfaceAddresses(ipv6MulticastChannel);
    }

    @Override
    protected void run() throws Exception {
        ByteBuffer recvBuffer = ByteBuffer.allocate(1100);
        
        while (true) {
            selector.select();
            if (stopFlag) {
                return;
            }
            
            for (SelectionKey key : selector.selectedKeys()) {
                if (!key.isValid()) {
                    continue;
                }
                
                DatagramChannel channel = (DatagramChannel) key.channel();
                
                if (key.isReadable()) {
                    CommunicationType commType;
                    if (channel == unicastChannel) {
                        commType = CommunicationType.UNICAST;
                    } else if (channel == ipv4MulticastChannel || channel == ipv6MulticastChannel) {
                        commType = CommunicationType.MULTICAST;
                    } else {
                        throw new IllegalStateException();
                    }
                    
                    recvBuffer.clear();
                    InetSocketAddress incomingAddress = (InetSocketAddress) channel.receive(recvBuffer);
                    if (incomingAddress != null && incomingAddress.getAddress().equals(gatewayAddress)) {
                        recvBuffer.flip();
                        for (NatPmpCommunicatorListener listener : listeners) {
                            try {
                                listener.incomingPacket(commType, recvBuffer.asReadOnlyBuffer());
                            } catch (RuntimeException re) { // NOPMD
                                // do nothing
                            }
                        }
                    }
                } else if (key.isWritable()) {
                    ByteBuffer sendBuffer = sendQueue.poll();
                    channel.send(sendBuffer, new InetSocketAddress(gatewayAddress, 5351));
                }
            }
            
            int nextOps = SelectionKey.OP_READ;
            if (!sendQueue.isEmpty()) {
                nextOps |= SelectionKey.OP_WRITE;
            }
            unicastChannel.register(selector, nextOps);
        }
    }

    @Override
    protected void shutDown() throws Exception {
        IOUtils.closeQuietly(selector);
        IOUtils.closeQuietly(unicastChannel);
        IOUtils.closeQuietly(ipv4MulticastChannel);
        IOUtils.closeQuietly(ipv6MulticastChannel);
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
