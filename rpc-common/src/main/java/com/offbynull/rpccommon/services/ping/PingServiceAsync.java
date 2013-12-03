package com.offbynull.rpccommon.services.ping;

import com.offbynull.rpc.invoke.AsyncResultListener;

public interface PingServiceAsync {
    void ping(AsyncResultListener<Long> result, long value);    
}
