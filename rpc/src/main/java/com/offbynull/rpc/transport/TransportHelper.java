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
package com.offbynull.rpc.transport;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

/**
 * A utility class for {@link Transport}s.
 * @author Kasra F
 */
public final class TransportHelper {
    /**
     * Marker object to indicate failure.
     */
    private static final Object FAILED_MARKER = new Object();
    
    private TransportHelper() {
        // do nothing
    }
    
    /**
     * Sends {@code message} through {@code transport} and waits for a response.
     * @param <A> address type
     * @param transport transport used to send {@code message}
     * @param message message to send
     * @return response, or {@code null} if timed out / internal error occurred
     * @throws InterruptedException if thread was interrupted
     * @throws NullPointerException if any arguments are {@code null}
     */
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

    /**
     * Sends {@code message} through {@code transport} and closes {@code transport} when it finishes, times out, or an error occurs.
     * @param <A> address type
     * @param transport transport used to send {@code message}
     * @param message message to send
     * @throws NullPointerException if any arguments are {@code null}
     */
    public static <A> void sendAndForget(final Transport<A> transport, OutgoingMessage<A> message) {
        Validate.notNull(transport);
        Validate.notNull(message);
        
        OutgoingMessageResponseListener<A> responseListener = new OutgoingMessageResponseListener<A>() {

            @Override
            public void responseArrived(IncomingResponse<A> response) {
                try {
                    transport.stop();
                } catch (Exception e) {
                    // do nothing
                }
            }

            @Override
            public void internalErrorOccurred(Throwable error) {
                try {
                    transport.stop();
                } catch (Exception e) {
                    // do nothing
                }
            }

            @Override
            public void timedOut() {
                try {
                    transport.stop();
                } catch (Exception e) {
                    // do nothing
                }
            }
        };
        
        transport.sendMessage(message, responseListener);
    }
}
