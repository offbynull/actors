package com.offbynull.p2prpc.io;

public interface ServerResponseCallback {
    void responseCompleted(byte[] data);
}
