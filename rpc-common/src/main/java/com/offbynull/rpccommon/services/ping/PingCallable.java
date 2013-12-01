package com.offbynull.rpccommon.services.ping;

import com.offbynull.rpc.Rpc;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.Validate;

public final class PingCallable<A> implements Callable<Long> {
    
    private A destination;
    private Rpc<A> rpc;

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
