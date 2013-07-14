package com.offbynull.p2prpc.invoke;

public interface CapturerCallback {
    byte[] invokationTriggered(byte[] data);
    void invokationFailed(Throwable err);
}
