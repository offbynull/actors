package com.offbynull.p2prpc.transport;

public interface OutgoingMessageResponseListener<A> {
    void responseArrived(IncomingResponse<A> response);
    void internalErrorOccurred(Throwable error);
}
