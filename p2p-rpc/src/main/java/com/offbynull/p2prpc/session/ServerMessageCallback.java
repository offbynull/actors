package com.offbynull.p2prpc.session;

public interface ServerMessageCallback<A> {
    void messageArrived(A from, byte[] data, ServerResponseCallback responseCallback);
}
