package com.offbynull.rpc.invoke;

public interface CapturerCallback {
    byte[] invokationTriggered(byte[] data);
    void invokationFailed(Throwable err);
}
