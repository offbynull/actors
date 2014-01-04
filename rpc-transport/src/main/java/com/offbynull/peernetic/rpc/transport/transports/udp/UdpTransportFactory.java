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
package com.offbynull.peernetic.rpc.transport.transports.udp;

import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transport.TransportFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

/**
 * Creates a {@link UdpTransport} based on properties in this class.
 * @author Kasra Faghihi
 */
public final class UdpTransportFactory implements TransportFactory<InetSocketAddress> {
    private int bufferSize = 65535;
    private int cacheSize = 4096;
    private long packetFlushTimeout = 10000L; 
    private long incomingResponseTimeout = 10000L; 
    private long outgoingResponseTimeout = 10000L; 
    private InetSocketAddress listenAddress = new InetSocketAddress(15000);

    /**
     * Sets the transport buffer size.
     * @param bufferSize buffer size
     * @throws IllegalArgumentException if {@code bufferSize < 0}
     */
    public void setBufferSize(int bufferSize) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, bufferSize);
        this.bufferSize = bufferSize;
    }

    /**
     * Sets the cache size for message ids.
     * @param cacheSize cache size
     * @throws IllegalArgumentException if {@code cacheSize < 0}
     */
    public void setMessageIdCacheSize(int cacheSize) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, cacheSize);
        this.cacheSize = cacheSize;
    }

    /**
     * Sets the packet flush timeout.
     * @param packetFlushTimeout timeout
     * @throws IllegalArgumentException if {@code packetFlushTimeout <= 0}
     */
    public void setPacketFlushTimeout(long packetFlushTimeout) {
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, packetFlushTimeout);
        this.packetFlushTimeout = packetFlushTimeout;
    }

    /**
     * Sets the incoming response timeout.
     * @param incomingResponseTimeout timeout
     * @throws IllegalArgumentException if {@code incomingResponseTimeout <= 0}
     */
    public void setIncomingResponseTimeout(long incomingResponseTimeout) {
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, incomingResponseTimeout);
        this.incomingResponseTimeout = incomingResponseTimeout;
    }

    /**
     * Sets the outgoing response timeout.
     * @param outgoingResponseTimeout timeout
     * @throws IllegalArgumentException if {@code outgoingResponseTimeout <= 0}
     */
    public void setOutgoingResponseTimeout(long outgoingResponseTimeout) {
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, outgoingResponseTimeout);
        this.outgoingResponseTimeout = outgoingResponseTimeout;
    }

    /**
     * Sets the listen address.
     * @param listenAddress listen address
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void setListenAddress(InetSocketAddress listenAddress) {
        Validate.notNull(listenAddress);
        this.listenAddress = listenAddress;
    }

    @Override
    public Transport<InetSocketAddress> createTransport() throws IOException {
        return new UdpTransport(listenAddress, bufferSize, cacheSize, packetFlushTimeout, outgoingResponseTimeout, incomingResponseTimeout);
    }
    
}
