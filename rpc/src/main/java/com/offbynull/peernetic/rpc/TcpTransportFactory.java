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
package com.offbynull.peernetic.rpc;

import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transports.tcp.TcpTransport;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

/**
 * Creates a {@link TcpTransport} based on properties in this class.
 * @author Kasra Faghihi
 */
public final class TcpTransportFactory implements TransportFactory<InetSocketAddress> {
    private int readLimit = 65535;
    private int writeLimit = 65535;
    private long timeout = 10000L; 
    private InetSocketAddress listenAddress = new InetSocketAddress(15000);

    /**
     * Gets the maximum number of bytes to read per message.
     * @return read limit
     */
    public int getReadLimit() {
        return readLimit;
    }

    /**
     * Sets the maximum number of bytes to read per message.
     * @param readLimit read limit
     * @throws IllegalArgumentException if {@code readLimit < 0}
     */
    public void setReadLimit(int readLimit) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, readLimit);
        this.readLimit = readLimit;
    }

    /**
     * Gets the maximum number of bytes to write per message.
     * @return write limit
     */
    public int getWriteLimit() {
        return writeLimit;
    }

    /**
     * Sets the maximum number of bytes to write per message.
     * @param writeLimit read limit
     * @throws IllegalArgumentException if {@code writeLimit < 0}
     */
    public void setWriteLimit(int writeLimit) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, writeLimit);
        this.writeLimit = writeLimit;
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
        return new TcpTransport(listenAddress, readLimit, writeLimit, timeout);
    }
    
}
