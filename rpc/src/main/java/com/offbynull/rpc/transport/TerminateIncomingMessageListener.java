package com.offbynull.rpc.transport;

public final class TerminateIncomingMessageListener<A> implements IncomingMessageListener<A> {

    @Override
    public void messageArrived(IncomingMessage<A> message, IncomingMessageResponseHandler responseCallback) {
        responseCallback.terminate();
    }
    
}
