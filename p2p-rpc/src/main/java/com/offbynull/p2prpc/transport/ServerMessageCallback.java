package com.offbynull.p2prpc.transport;

public interface ServerMessageCallback<A> {
    void messageArrived(A from, byte[] data, ServerResponseCallback responseCallback);
}
