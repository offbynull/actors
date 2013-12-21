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
package com.offbynull.rpccommon.services.ping;

import com.offbynull.rpc.Rpc;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.Validate;

/**
 * Encapsulates the logic to query a {@link PingService}.
 * @author Kasra F
 * @param <A> address type
 */
public final class PingCallable<A> implements Callable<Long> {
    
    private A destination;
    private Rpc<A> rpc;

    /**
     * Constructs a {@link PingCallable} object.
     * @param destination address to ping
     * @param rpc rpc
     */
    public PingCallable(A destination, Rpc<A> rpc) {
        Validate.notNull(destination);
        Validate.notNull(rpc);
        
        this.destination = destination;
        this.rpc = rpc;
    }

    @Override
    public Long call() throws Exception {
        PingService service = rpc.accessService(destination, PingService.SERVICE_ID, PingService.class);
        
        long startTimestamp = System.currentTimeMillis();
        long retValue = service.ping(startTimestamp);
        long stopTimestamp = System.currentTimeMillis();
        
        if (retValue != startTimestamp) {
            throw new IllegalStateException("Value to destination was not echod");
        }
        
        return Math.min(0L, stopTimestamp - startTimestamp); // just incase, clock may not be accurate
    }
    
}
