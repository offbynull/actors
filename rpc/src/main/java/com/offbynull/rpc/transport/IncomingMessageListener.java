package com.offbynull.rpc.transport;

public interface IncomingMessageListener<A> {
    void messageArrived(IncomingMessage<A> message, IncomingMessageResponseHandler responseCallback);
}
