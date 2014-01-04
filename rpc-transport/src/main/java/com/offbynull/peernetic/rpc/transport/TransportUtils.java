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
package com.offbynull.peernetic.rpc.transport;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

/**
 * A utility class for {@link Transport}s.
 * @author Kasra Faghihi
 */
public final class TransportUtils {
    /**
     * Marker object to indicate failure.
     */
    private static final Object FAILED_MARKER = new Object();
    
    private TransportUtils() {
        // do nothing
    }
    
    /**
     * Sends {@code message} through {@code transport} and waits for a response.
     * @param <A> address type
     * @param transport transport used to send {@code message}
     * @param to destination
     * @param data message contents
     * @return response, or {@code null} if timed out / internal error occurred
     * @throws InterruptedException if thread was interrupted
     * @throws NullPointerException if any arguments are {@code null}
     */
    public static <A> ByteBuffer sendAndWait(Transport<A> transport, A to, ByteBuffer data) throws InterruptedException {
        Validate.notNull(transport);
        Validate.notNull(to);
        Validate.notNull(data);
        
        final ArrayBlockingQueue<Object> barrier = new ArrayBlockingQueue<>(1);
        OutgoingMessageResponseListener responseListener = new OutgoingMessageResponseListener() {

            @Override
            public void responseArrived(ByteBuffer response) {
                barrier.add(response);
            }

            @Override
            public void errorOccurred(Object error) {
                barrier.add(FAILED_MARKER);
            }
        };
        
        transport.sendMessage(to, data, responseListener);
        
        Object resp = barrier.poll(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        
        if (resp == FAILED_MARKER) {
            return null;
        }
        
        return (ByteBuffer) resp;
    }
}