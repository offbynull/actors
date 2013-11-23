package com.offbynull.p2prpc;

import com.offbynull.p2prpc.transport.Transport;
import com.offbynull.p2prpc.transport.udp.UdpTransport;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

public final class UdpTransportFactory implements TransportFactory<InetSocketAddress> {
    private int bufferSize = 65535;
    private int cacheSize = 4096;
    private long timeout = 10000L; 
    private InetSocketAddress listenAddress = new InetSocketAddress(15000);

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, bufferSize);
        this.bufferSize = bufferSize;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, cacheSize);
        this.cacheSize = cacheSize;
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
        return new UdpTransport(listenAddress, bufferSize, cacheSize, timeout);
    }
    
}
