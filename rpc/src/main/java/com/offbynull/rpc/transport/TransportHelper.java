package com.offbynull.rpc.transport;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

public final class TransportHelper {
    private static final Object FAILED_MARKER = new Object();
    
    private TransportHelper() {
        // do nothing
    }
    
    public static <A> IncomingResponse<A> sendAndWait(Transport<A> transport, OutgoingMessage<A> message) throws InterruptedException {
        Validate.notNull(transport);
        Validate.notNull(message);
        
        final ArrayBlockingQueue<Object> barrier = new ArrayBlockingQueue<>(1);
        OutgoingMessageResponseListener<A> responseListener = new OutgoingMessageResponseListener<A>() {

            @Override
            public void responseArrived(IncomingResponse<A> response) {
                barrier.add(response);
            }

            @Override
            public void internalErrorOccurred(Throwable error) {
                barrier.add(FAILED_MARKER);
            }

            @Override
            public void timedOut() {
                barrier.add(FAILED_MARKER);
            }
        };
        
        transport.sendMessage(message, responseListener);
        
        Object resp = barrier.poll(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        
        if (resp == FAILED_MARKER) {
            return null;
        }
        
        return (IncomingResponse<A>) resp;
    }
}
