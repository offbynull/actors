package com.offbynull.rpccommon.services.ping;

public interface PingService {
    public static final int SERVICE_ID = 2001;
    
    long ping(long value);
}
