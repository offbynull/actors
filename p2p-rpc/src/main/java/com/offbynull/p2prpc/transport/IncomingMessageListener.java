package com.offbynull.p2prpc.transport;

public interface IncomingMessageListener<A> {
    void messageArrived(IncomingMessage<A> message, IncomingMessageResponseHandler responseCallback);
}
