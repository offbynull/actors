package com.offbynull.rpc.invoke;

public interface AsyncResultListener<T> {
    void invokationReturned(T object);
    void invokationThrew(Throwable err);
    void invokationFailed(Object err);
}
