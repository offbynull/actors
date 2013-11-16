package com.offbynull.p2prpc.session;

public interface MessageListener<A> {
    void messageArrived(A from, byte[] data, ResponseHandler responseCallback);
}
