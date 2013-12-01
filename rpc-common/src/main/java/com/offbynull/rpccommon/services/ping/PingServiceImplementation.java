package com.offbynull.rpccommon.services.ping;

public final class PingServiceImplementation implements PingService {

    @Override
    public long ping(long value) {
        return value;
    }
    
}
