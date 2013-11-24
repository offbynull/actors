package com.offbynull.rpc.transport;

/**
 * A listener that gets triggered when a new message arrives. Implementations must be thread-safe.
 * @author Kasra F
 * @param <A> address type
 */
public interface IncomingMessageListener<A> {
    /**
     * Indicates that a message has arrived. Implementations of this method must not block.
     * @param message message
     * @param responseCallback response handler
     * @throws NullPointerException if any of the arguments are {@code null}
     */
    void messageArrived(IncomingMessage<A> message, IncomingMessageResponseHandler responseCallback);
}
