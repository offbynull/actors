package com.offbynull.p2prpc.session;

public interface ServerResponseCallback {
    void responseReady(byte[] data);
    void terminate();
}
