package com.offbynull.rpc.invoke;

public interface InvokerCallback {
    void invokationFailed(Throwable t);
    void invokationFinised(byte[] data);
}
