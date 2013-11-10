package com.offbynull.p2prpc.service;

import com.offbynull.p2prpc.invoke.Capturer;
import com.offbynull.p2prpc.invoke.CapturerCallback;
import com.offbynull.p2prpc.session.Client;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class ServiceAccessor<A> {
    private Client<A> client;

    public ServiceAccessor(Client<A> client) {
        Validate.notNull(client);
        
        this.client = client;
    }

    public <T> T accessService(A address, int serviceId, Class<T> type) {
        return accessService(address, serviceId, type, 10000L);
    }
    
    public <T> T accessService(A address, int serviceId, Class<T> type, long timeout) {
        return accessService(address, serviceId, type, timeout, new RuntimeException("Comm failure"),
                new RuntimeException("Invoke failure"));
    }
    
    public <T> T accessService(final A address, final int serviceId, Class<T> type, final long timeout,
            final RuntimeException throwOnCommFailure, final RuntimeException throwOnInvokeFailure) {
        Validate.notNull(address);
        Validate.notNull(type);
        Validate.notNull(throwOnCommFailure);
        Validate.notNull(throwOnInvokeFailure);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);
        
        
        Capturer<T> capturer = new Capturer<>(type);
        T obj = capturer.createInstance(new CapturerCallback() {

            @Override
            public byte[] invokationTriggered(byte[] data) {
                try {
                    ByteBuffer buffer = ByteBuffer.allocate(data.length + 4);
                    buffer.putInt(serviceId);
                    buffer.put(data);
                    
                    byte[] response = client.send(address, buffer.array(), timeout);
                    return response;
                } catch (IOException | InterruptedException ex) {
                    Thread.interrupted(); // ignore interrupt
                    throw throwOnCommFailure;
                }
            }

            @Override
            public void invokationFailed(Throwable err) {
                throw throwOnInvokeFailure;
            }
        });

        return obj;
    }
}
