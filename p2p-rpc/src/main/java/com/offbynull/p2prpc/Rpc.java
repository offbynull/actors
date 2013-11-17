package com.offbynull.p2prpc;

import com.offbynull.p2prpc.transport.Transport;
import com.offbynull.p2prpc.transport.tcp.TcpTransport;
import com.offbynull.p2prpc.transport.udp.UdpTransport;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

public final class Rpc implements Closeable {

    private boolean closed;
    
    private Transport<InetSocketAddress> transport;

    private ServiceServer<InetSocketAddress> serviceServer;
    private ServiceAccessor<InetSocketAddress> serviceAccessor;

    public Rpc() throws IOException {
        this(new RpcConfig());
    }

    public Rpc(RpcConfig conf) throws IOException {
        Validate.notNull(conf);

        try {
            switch (conf.getType()) {
                case TCP: {
                    TcpTransport tcpTransport = new TcpTransport(conf.getTcpListenAddress(), conf.getTcpReadLimit(),
                            conf.getTcpWriteLimit(), conf.getTcpTimeout());
                    tcpTransport.start();
                    transport = tcpTransport;
                    break;
                }
                case UDP: {
                    UdpTransport udpTransport = new UdpTransport(conf.getUdpListenAddress(), conf.getUdpBufferSize(), conf.getUdpTimeout());
                    udpTransport.start();
                    transport = udpTransport;
                    break;
                }
                default:
                    throw new IllegalArgumentException();
            }

            serviceServer = new ServiceServer<>(transport, conf.getInvokerExecutorService());
            serviceAccessor = new ServiceAccessor<>(transport);
            
            serviceServer.start();
            
            closed = false;
        } catch (IOException | RuntimeException ex) {
            closed = true;
            close();
            throw ex;
        }
    }

    private void stopTransport() throws IOException {
        if (transport instanceof TcpTransport) {
            ((TcpTransport) transport).stop();
        } else if (transport instanceof UdpTransport) {
            ((UdpTransport) transport).stop();
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void close() {
        closed = true;
        
        try {
            serviceServer.stop();
        } catch (Exception ex) {
            // do nothing
        }

        try {
            stopTransport();
        } catch (Exception ex) {
            // do nothing
        }
    }

    public void addService(int id, Object object) {
        if (closed) {
            throw new IllegalStateException();
        }
        
        serviceServer.addService(id, object);
    }

    public void removeService(int id) {
        if (closed) {
            throw new IllegalStateException();
        }
        
        serviceServer.removeService(id);
    }

    public <T> T accessService(InetSocketAddress address, int serviceId, Class<T> type) {
        if (closed) {
            throw new IllegalStateException();
        }
        
        return serviceAccessor.accessService(address, serviceId, type);
    }

    public <T> T accessService(InetSocketAddress address, int serviceId, Class<T> type, long timeout) {
        if (closed) {
            throw new IllegalStateException();
        }
        
        return serviceAccessor.accessService(address, serviceId, type, timeout);
    }

    public <T> T accessService(InetSocketAddress address, int serviceId, Class<T> type, long timeout,
            RuntimeException throwOnCommFailure, RuntimeException throwOnInvokeFailure) {
        if (closed) {
            throw new IllegalStateException();
        }
        
        return serviceAccessor.accessService(address, serviceId, type, timeout, throwOnCommFailure, throwOnInvokeFailure);
    }
}
