package com.offbynull.rpc.transport;

/**
 * An {@link IncomingMessageListener} implementation that responds with a termination as soon as a message arrives.
 * @author Kasra F
 * @param <A> address type
 */
public final class TerminateIncomingMessageListener<A> implements IncomingMessageListener<A> {

    @Override
    public void messageArrived(IncomingMessage<A> message, IncomingMessageResponseHandler responseCallback) {
        responseCallback.terminate();
    }
    
}
