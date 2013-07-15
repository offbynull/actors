package com.offbynull.p2prpc.transport;

public interface ServerResponseCallback {
    void responseCompleted(byte[] data);
}
