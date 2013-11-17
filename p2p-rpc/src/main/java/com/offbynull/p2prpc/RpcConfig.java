package com.offbynull.p2prpc;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

public class RpcConfig {
    
    private TransportType type = TransportType.TCP;
    private int tcpReadLimit = 65535;
    private int tcpWriteLimit = 65535;
    private long tcpTimeout = 10000L; 
    private InetSocketAddress tcpListenAddress = new InetSocketAddress(15000);
    private int udpBufferSize = 65535;
    private long udpTimeout = 10000L; 
    private int udpIdCache = 2048; 
    private InetSocketAddress udpListenAddress = new InetSocketAddress(15000);
    private ExecutorService invokerExecutorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 1, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());
    

    public TransportType getType() {
        return type;
    }

    public void setType(TransportType type) {
        Validate.notNull(type);
        this.type = type;
    }

    public int getTcpReadLimit() {
        return tcpReadLimit;
    }

    public void setTcpReadLimit(int tcpReadLimit) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, tcpReadLimit);
        this.tcpReadLimit = tcpReadLimit;
    }

    public int getTcpWriteLimit() {
        return tcpWriteLimit;
    }

    public void setTcpWriteLimit(int tcpWriteLimit) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, tcpWriteLimit);
        this.tcpWriteLimit = tcpWriteLimit;
    }

    public InetSocketAddress getTcpListenAddress() {
        return tcpListenAddress;
    }

    public void setTcpListenAddress(InetSocketAddress tcpListenAddress) {
        Validate.notNull(tcpListenAddress);
        this.tcpListenAddress = tcpListenAddress;
    }

    public int getUdpBufferSize() {
        return udpBufferSize;
    }

    public void setUdpBufferSize(int udpBufferSize) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, udpBufferSize);
        this.udpBufferSize = udpBufferSize;
    }

    public int getUdpIdCache() {
        return udpIdCache;
    }

    public void setUdpIdCache(int udpIdCache) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, udpIdCache);
        this.udpIdCache = udpIdCache;
    }

    public InetSocketAddress getUdpListenAddress() {
        return udpListenAddress;
    }

    public void setUdpListenAddress(InetSocketAddress udpListenAddress) {
        Validate.notNull(udpListenAddress);
        this.udpListenAddress = udpListenAddress;
    }

    public ExecutorService getInvokerExecutorService() {
        return invokerExecutorService;
    }

    public void setInvokerExecutorService(ExecutorService invokerExecutorService) {
        Validate.notNull(invokerExecutorService);
        this.invokerExecutorService.shutdownNow();
        this.invokerExecutorService = invokerExecutorService;
    }

    public long getTcpTimeout() {
        return tcpTimeout;
    }

    public void setTcpTimeout(long tcpTimeout) {
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, tcpTimeout);
        this.tcpTimeout = tcpTimeout;
    }

    public long getUdpTimeout() {
        return udpTimeout;
    }

    public void setUdpTimeout(long udpTimeout) {
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, udpTimeout);
        this.udpTimeout = udpTimeout;
    }
    
    public enum TransportType {

        TCP,
        UDP
    }
}
