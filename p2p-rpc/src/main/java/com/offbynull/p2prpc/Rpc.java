package com.offbynull.p2prpc;

import com.offbynull.p2prpc.transport.IncomingMessageListener;
import com.offbynull.p2prpc.transport.Transport;
import java.io.Closeable;
import java.io.IOException;
import org.apache.commons.lang3.Validate;

public final class Rpc<A> implements Closeable {

    private boolean closed;
    
    private Transport<A> transport;

    private ServiceRouter<A> serviceRouter;
    private ServiceAccessor<A> serviceAccessor;

    public Rpc(TransportFactory<A> transportFactory) throws IOException {
        this(transportFactory, new RpcConfig<A>());
    }
    
    public Rpc(TransportFactory<A> transportFactory, RpcConfig<A> conf) throws IOException {
        Validate.notNull(conf);
        Validate.notNull(transportFactory);

        try {
            transport = transportFactory.createTransport();

            serviceRouter = new ServiceRouter<>(conf.getInvokerExecutorService());
            serviceAccessor = new ServiceAccessor<>(transport);
            
            IncomingMessageListener<A> listener = serviceRouter.getIncomingMessageListener();
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

    public <T> T accessService(A address, int serviceId, Class<T> type) {
        if (closed) {
            throw new IllegalStateException();
        }
        
        return serviceAccessor.accessService(address, serviceId, type);
    }

    public <T> T accessService(A address, int serviceId, Class<T> type, long timeout) {
        if (closed) {
            throw new IllegalStateException();
        }
        
        return serviceAccessor.accessService(address, serviceId, type, timeout);
    }

    public <T> T accessService(A address, int serviceId, Class<T> type, long timeout,
            RuntimeException throwOnCommFailure, RuntimeException throwOnInvokeFailure) {
        if (closed) {
            throw new IllegalStateException();
        }
        
        return serviceAccessor.accessService(address, serviceId, type, timeout, throwOnCommFailure, throwOnInvokeFailure);
    }
}
