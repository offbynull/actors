package com.offbynull.rpc;

import com.offbynull.rpc.invoke.Capturer;
import com.offbynull.rpc.invoke.CapturerHandler;
import com.offbynull.rpc.transport.IncomingResponse;
import com.offbynull.rpc.transport.OutgoingMessage;
import com.offbynull.rpc.transport.Transport;
import com.offbynull.rpc.transport.TransportHelper;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

final class ServiceAccessor<A> {
    private Transport<A> transport;

    public ServiceAccessor(Transport<A> transport) {
        Validate.notNull(transport);
        
        this.transport = transport;
    }
    
    public <T> T accessService(final A address, final int serviceId, Class<T> type, final RuntimeException throwOnCommFailure,
            final RuntimeException throwOnInvokeFailure) {
        Validate.notNull(address);
        Validate.notNull(type);
        Validate.notNull(throwOnCommFailure);
        Validate.notNull(throwOnInvokeFailure);
        
        
        Capturer<T> capturer = new Capturer<>(type);
        T obj = capturer.createInstance(new CapturerHandler() {

            @Override
            public byte[] invokationTriggered(byte[] data) {
                try {
                    ByteBuffer buffer = ByteBuffer.allocate(data.length + 4);
                    buffer.putInt(serviceId);
                    buffer.put(data);
                    buffer.position(0);
                    
                    OutgoingMessage<A> message = new OutgoingMessage<>(address, buffer);
                    
                    IncomingResponse<A> response = TransportHelper.sendAndWait(transport, message);
                    ByteBuffer resp = response.getData();
                    byte[] respArray = new byte[resp.remaining()];
                    resp.get(respArray);
                    
                    return respArray;
                } catch (Exception ex) {
                    Thread.interrupted(); // ignore interrupt, if it's interrupted
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
