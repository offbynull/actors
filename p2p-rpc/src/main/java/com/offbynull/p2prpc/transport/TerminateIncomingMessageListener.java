package com.offbynull.p2prpc.transport;

public final class TerminateIncomingMessageListener<A> implements IncomingMessageListener<A> {

    @Override
    public void messageArrived(IncomingMessage<A> message, IncomingMessageResponseHandler responseCallback) {
        responseCallback.terminate();
    }
    
}
