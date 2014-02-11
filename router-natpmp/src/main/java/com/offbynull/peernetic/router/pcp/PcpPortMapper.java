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
package com.offbynull.peernetic.router.pcp;

import com.google.common.util.concurrent.ForwardingFuture.SimpleForwardingFuture;
import com.offbynull.peernetic.router.MappedPort;
import com.offbynull.peernetic.router.Port;
import com.offbynull.peernetic.router.PortMapper;
import com.offbynull.peernetic.router.PortMapperEventListener;
import com.offbynull.peernetic.router.pcp.PcpPortMapperService.MapPortMessage;
import com.offbynull.peernetic.router.pcp.PcpPortMapperService.UnmapPortMessage;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

/**
 * A PCP {@link PortMapper} implementation.
 * @author Kasra Faghihi
 */
public final class PcpPortMapper extends PortMapper {
    
    private PcpPortMapperService service;
    private LinkedBlockingQueue<Object> msgQueue;
    private Lock lock;

    public PcpPortMapper(PortMapperEventListener portMapperListener, InetAddress gatewayAddress, InetAddress selfAddress) {
        super(portMapperListener);
        Validate.notNull(portMapperListener);
        Validate.notNull(gatewayAddress);
        Validate.notNull(selfAddress);
        lock = new ReentrantLock();
        msgQueue = new LinkedBlockingQueue<>();
        service = new PcpPortMapperService(msgQueue, portMapperListener, gatewayAddress, selfAddress);
        service.startAsync().awaitRunning();
    }

    @Override
    public Future<MappedPort> mapPort(Port port) {
        lock.lock();
        try {
            Validate.validState(service.isRunning());
            
            MapPortMessage message = new MapPortMessage(port);
            msgQueue.add(message);
            
            return new SimpleForwardingFuture<MappedPort>(message.getFuture()) { };
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Future<Void> unmapPort(Port port) {
        lock.lock();
        try {
            Validate.validState(service.isRunning());
            
            UnmapPortMessage message = new UnmapPortMessage(port);
            msgQueue.add(message);
            
            return new SimpleForwardingFuture<Void>(message.getFuture()) { };
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            service.stopAsync().awaitTerminated();
        } catch (IllegalStateException ise) {
            throw new IOException(ise);
        } finally {
            lock.unlock();
        }
    }
}
