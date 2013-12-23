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

import com.offbynull.peernetic.rpc.invoke.AsyncResultListener;
import com.offbynull.peernetic.rpc.transport.CompositeIncomingFilter;
import com.offbynull.peernetic.rpc.transport.CompositeOutgoingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import com.offbynull.peernetic.rpc.transport.Transport;
import java.io.Closeable;
import java.io.IOException;
import org.apache.commons.lang3.Validate;

/**
 * RPC entry point.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class Rpc<A> implements Closeable {

    private boolean closed;

    private Transport<A> transport;

    private ServiceRouter<A> serviceRouter;
    private ServiceAccessor<A> serviceAccessor;
    
    /**
     * Constructs a {@link Rpc} object with default configuration.
     * @param transportFactory transport factory
     * @throws IOException on error
     * @throws NullPointerException if any arguments is {@code null}
     */
    public Rpc(TransportFactory<A> transportFactory) throws IOException {
        this(transportFactory, new RpcConfig<A>());
    }

    /**
     * Constructs a {@link Rpc} object.
     * @param transportFactory transport factory
     * @param conf configuration
     * @throws IOException on error
     * @throws NullPointerException if any arguments is {@code null}
     */
    public Rpc(TransportFactory<A> transportFactory, RpcConfig<A> conf) throws IOException {
        Validate.notNull(conf);
        Validate.notNull(transportFactory);

        try {
            transport = transportFactory.createTransport();

            serviceRouter = new ServiceRouter<>(conf.getInvokerExecutorService(), this, conf.getExtraInvokeInfo());
            serviceAccessor = new ServiceAccessor<>(transport);

            IncomingMessageListener<A> listener = serviceRouter.getIncomingMessageListener();
            
            IncomingFilter<A> inFilter = new CompositeIncomingFilter<>(conf.getIncomingFilters());
            OutgoingFilter<A> outFilter = new CompositeOutgoingFilter<>(conf.getOutgoingFilters());
            transport.start(inFilter, listener, outFilter);
            
            closed = false;
        } catch (IOException | RuntimeException ex) {
            closed = true;
            close();
            throw new IOException(ex);
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;

        try {
            transport.stop();
        } catch (IOException | RuntimeException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Add a service.
     * @param id service id
     * @param object service object
     * @throws IllegalStateException if closed
     * @throws IllegalArgumentException if {@code id} is 0
     * @throws NullPointerException if {@code object} is {@code null}
     */
    public void addService(int id, Object object) {
        Validate.validState(!closed);

        serviceRouter.addService(id, object);
    }

    /**
     * Remove a service.
     * @param id service id
     * @throws IllegalStateException if closed
     * @throws IllegalArgumentException if {@code id} is 0
     */
    public void removeService(int id) {
        Validate.validState(!closed);

        serviceRouter.removeService(id);
    }

    /**
     * Access a remote service asynchronously. Throws a {@link RuntimeException} on communication/invokation failure. Must not block in
     * {@link AsyncResultListener}.
     * @param address destination address
     * @param id service id
     * @param type service type class
     * @param asyncType service async type class
     * @param <T> service type
     * @param <AT> asynchronous service type
     * @return an object of type {@code type} that accesses the service at {@code id}
     * @throws IllegalStateException if closed
     * @throws NullPointerException if any arguments are {@code null}
     */
    public <T, AT> AT accessService(A address, int id, Class<T> type, Class<AT> asyncType) {
        Validate.validState(!closed);

        return serviceAccessor.accessServiceAsync(address, id, type, asyncType, new RuntimeException("Comm failure"),
                new RuntimeException("Invoke failure"));
    }

    /**
     * Access a remote service asynchronously. Must not block in {@link AsyncResultListener}.
     * @param address destination address
     * @param id service id
     * @param type service type class
     * @param asyncType service async type class
     * @param throwOnCommFailure exception to throw on communication failure
     * @param throwOnInvokeFailure exception to throw on invokation failure
     * @param <T> service type
     * @param <AT> asynchronous service type
     * @return an object of type {@code type} that accesses the service at {@code id}
     * @throws IllegalStateException if closed
     * @throws IllegalArgumentException if {@code timeout < 1L} 
     * @throws NullPointerException if any arguments are {@code null}
     */
    public <T, AT> AT accessService(A address, int id, Class<T> type, Class<AT> asyncType, RuntimeException throwOnCommFailure,
            RuntimeException throwOnInvokeFailure) {
        Validate.validState(!closed);

        return serviceAccessor.accessServiceAsync(address, id, type, asyncType, throwOnCommFailure, throwOnInvokeFailure);
    }

    /**
     * Access a remote service. Times out after 10 seconds. Throws a {@link RuntimeException} on communication/invokation failure.
     * @param address destination address
     * @param id service id
     * @param type service type class
     * @param <T> service type
     * @return an object of type {@code type} that accesses the service at {@code id}
     * @throws IllegalStateException if closed
     * @throws NullPointerException if any arguments are {@code null}
     */
    public <T> T accessService(A address, int id, Class<T> type) {
        Validate.validState(!closed);

        return serviceAccessor.accessService(address, id, type, new RuntimeException("Comm failure"),
                new RuntimeException("Invoke failure"));
    }

    /**
     * Access a remote service.
     * @param address destination address
     * @param id service id
     * @param type service type class
     * @param throwOnCommFailure exception to throw on communication failure
     * @param throwOnInvokeFailure exception to throw on invokation failure
     * @param <T> service type
     * @return an object of type {@code type} that accesses the service at {@code id}
     * @throws IllegalStateException if closed
     * @throws IllegalArgumentException if {@code timeout < 1L} 
     * @throws NullPointerException if any arguments are {@code null}
     */
    public <T> T accessService(A address, int id, Class<T> type, RuntimeException throwOnCommFailure,
            RuntimeException throwOnInvokeFailure) {
        Validate.validState(!closed);

        return serviceAccessor.accessService(address, id, type, throwOnCommFailure, throwOnInvokeFailure);
    }
}
