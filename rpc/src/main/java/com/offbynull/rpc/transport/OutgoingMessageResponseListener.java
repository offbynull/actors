package com.offbynull.rpc.transport;

public interface OutgoingMessageResponseListener<A> {
    void responseArrived(IncomingResponse<A> response);
    void internalErrorOccurred(Throwable error);
    void timedOut();
}
