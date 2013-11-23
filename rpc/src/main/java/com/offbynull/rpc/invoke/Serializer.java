package com.offbynull.rpc.invoke;

public interface Serializer {
    byte[] serializeMethodCall(InvokeData invokeData);
    byte[] serializeMethodReturn(Object ret);
    byte[] serializeMethodThrow(Throwable err);
}
