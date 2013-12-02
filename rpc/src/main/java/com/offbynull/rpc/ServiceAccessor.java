package com.offbynull.rpc;

import com.offbynull.rpc.invoke.AsyncCapturer;
import com.offbynull.rpc.invoke.AsyncCapturerHandler;
import com.offbynull.rpc.invoke.AsyncCapturerHandlerCallback;
import com.offbynull.rpc.invoke.Capturer;
import com.offbynull.rpc.invoke.CapturerHandler;
import com.offbynull.rpc.transport.IncomingResponse;
import com.offbynull.rpc.transport.OutgoingMessage;
import com.offbynull.rpc.transport.OutgoingMessageResponseListener;
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

    public <T, AT> AT accessServiceAsync(final A address, final int serviceId, Class<T> type, Class<AT> asyncType,
            final RuntimeException throwOnCommFailure, final RuntimeException throwOnInvokeFailure) {
        Validate.notNull(address);
        Validate.notNull(type);
        Validate.notNull(asyncType);
        Validate.notNull(throwOnCommFailure);
        Validate.notNull(throwOnInvokeFailure);
        
        
        AsyncCapturer<T, AT> capturer = new AsyncCapturer<>(type, asyncType);
        AT obj = capturer.createInstance(new AsyncCapturerHandler() {

            @Override
            public void invokationFailed(Throwable err) {
                throw throwOnInvokeFailure;
            }

            @Override
            public void invokationTriggered(byte[] data, final AsyncCapturerHandlerCallback responseHandler) {
                try {
                    ByteBuffer buffer = ByteBuffer.allocate(data.length + 4);
                    buffer.putInt(serviceId);
                    buffer.put(data);
                    buffer.position(0);
                    
                    OutgoingMessage<A> message = new OutgoingMessage<>(address, buffer);
                    
                    transport.sendMessage(message, new OutgoingMessageResponseListener<A>() {

                        @Override
                        public void responseArrived(IncomingResponse<A> response) {
                            ByteBuffer resp = response.getData();
                            byte[] respArray = new byte[resp.remaining()];
                            resp.get(respArray);
                            
                            responseHandler.responseArrived(respArray);
                        }

                        @Override
                        public void internalErrorOccurred(Throwable error) {
                            responseHandler.responseFailed(throwOnCommFailure);
                        }

                        @Override
                        public void timedOut() {
                            responseHandler.responseFailed(throwOnCommFailure);
                        }
                    });
                } catch (RuntimeException ex) {
                    responseHandler.responseFailed(throwOnInvokeFailure);
                }
            }
        });

        return obj;
    }
}
