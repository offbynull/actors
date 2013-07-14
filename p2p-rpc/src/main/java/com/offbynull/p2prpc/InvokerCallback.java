package com.offbynull.p2prpc;

public interface InvokerCallback {
    void methodReturned(Object retVal);
    void methodErrored(Throwable throwable);
    void invokationErrored(Throwable throwable);
}
