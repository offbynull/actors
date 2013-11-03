package com.offbynull.p2prpc.transport;

public interface ServerResponseCallback {
    void responseReady(byte[] data);
    void terminate();
}
