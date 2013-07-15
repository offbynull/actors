package com.offbynull.p2prpc.io;

public interface ServerMessageCallback {
    void messageArrived(byte[] data, ServerResponseCallback responseCallback);
}
