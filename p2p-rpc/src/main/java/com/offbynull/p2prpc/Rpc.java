package com.offbynull.p2prpc;

import com.offbynull.p2prpc.transport.IncomingMessageListener;
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

    private ServiceRouter<InetSocketAddress> serviceRouter;
    private ServiceAccessor<InetSocketAddress> serviceAccessor;

    public Rpc() throws IOException {
        this(new RpcConfig());
    }

    public Rpc(RpcConfig conf) throws IOException {
        Validate.notNull(conf);

        try {
            switch (conf.getType()) {
                case TCP: {
                    transport = new TcpTransport(conf.getTcpListenAddress(), conf.getTcpReadLimit(),
                            conf.getTcpWriteLimit(), conf.getTcpTimeout());
                    break;
                }
                case UDP: {
                    transport = new UdpTransport(conf.getUdpListenAddress(), conf.getUdpBufferSize(), conf.getUdpIdCache(),
                            conf.getUdpTimeout());
                    break;
                }
                default:
                    throw new IllegalArgumentException();
            }

            serviceRouter = new ServiceRouter<>(conf.getInvokerExecutorService());
            serviceAccessor = new ServiceAccessor<>(transport);
            
            IncomingMessageListener<InetSocketAddress> listener = serviceRouter.getIncomingMessageListener();
            transport.start(listener);
            
            closed = false;
        } catch (IOException | RuntimeException ex) {
            closed = true;
            close();
            throw ex;
        }
    }

    @Override
    public void close() {
        closed = true;

        try {
            transport.stop();
        } catch (Exception ex) {
            // do nothing
        }
    }

    public void addService(int id, Object object) {
        if (closed) {
            throw new IllegalStateException();
        }
        
        serviceRouter.addService(id, object);
    }

    public void removeService(int id) {
        if (closed) {
            throw new IllegalStateException();
        }
        
        serviceRouter.removeService(id);
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
