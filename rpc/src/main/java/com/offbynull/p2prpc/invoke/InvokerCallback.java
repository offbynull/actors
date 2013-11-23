package com.offbynull.p2prpc.invoke;

public interface InvokerCallback {
    void invokationFailed(Throwable t);
    void invokationFinised(byte[] data);
}
