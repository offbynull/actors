package com.offbynull.p2prpc.transport;

public interface IncomingMessageResponseHandler {
    void responseReady(OutgoingResponse response);
    void terminate();
}
