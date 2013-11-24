package com.offbynull.rpc;

import com.offbynull.rpc.transport.Transport;
import com.offbynull.rpc.transport.tcp.TcpTransport;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

/**
 * Creates a {@link TcpTransport} based on properties in this class.
 * @author Kasra F
 */
public final class TcpTransportFactory implements TransportFactory<InetSocketAddress> {
    private int readLimit = 65535;
    private int writeLimit = 65535;
    private long timeout = 10000L; 
    private InetSocketAddress listenAddress = new InetSocketAddress(15000);

    public int getReadLimit() {
        return readLimit;
    }

    public void setReadLimit(int readLimit) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, readLimit);
        this.readLimit = readLimit;
    }

    public int getWriteLimit() {
        return writeLimit;
    }

    public void setWriteLimit(int writeLimit) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, writeLimit);
        this.writeLimit = writeLimit;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);
        this.timeout = timeout;
    }

    public InetSocketAddress getListenAddress() {
        return listenAddress;
    }

    public void setListenAddress(InetSocketAddress listenAddress) {
        Validate.notNull(listenAddress);
        this.listenAddress = listenAddress;
    }

    @Override
    public Transport<InetSocketAddress> createTransport() throws IOException {
        return new TcpTransport(listenAddress, readLimit, writeLimit, timeout);
    }
    
}
