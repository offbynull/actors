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

import com.offbynull.peernetic.rpc.transport.TransportFactory;
import com.offbynull.peernetic.rpc.transport.Transport;
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
    private long timeout = 10000L; 
    private InetSocketAddress listenAddress = new InetSocketAddress(15000);

    /**
     * Gets the buffer size.
     * @return buffer size
     */
    public int getBufferSize() {
        return bufferSize;
    }

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
     * Gets the cache size for message ids.
     * @return cache size
     */
    public int getMessageIdCacheSize() {
        return cacheSize;
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
     * Gets the timeout.
     * @return timeout
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * Sets the timeout.
     * @param timeout timeout
     * @throws IllegalArgumentException if {@code timeout <= 0}
     */
    public void setTimeout(long timeout) {
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);
        this.timeout = timeout;
    }

    /**
     * Gets the listen address.
     * @return listen address
     */
    public InetSocketAddress getListenAddress() {
        return listenAddress;
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
        return new UdpTransport(listenAddress, bufferSize, cacheSize, timeout);
    }
    
}
