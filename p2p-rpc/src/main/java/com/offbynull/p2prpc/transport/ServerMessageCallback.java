package com.offbynull.p2prpc.transport;

public interface ServerMessageCallback {
    void messageArrived(byte[] data, ServerResponseCallback responseCallback);
}
