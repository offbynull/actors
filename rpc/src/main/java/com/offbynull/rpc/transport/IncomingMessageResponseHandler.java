package com.offbynull.rpc.transport;

public interface IncomingMessageResponseHandler {
    void responseReady(OutgoingResponse response);
    void terminate();
}
