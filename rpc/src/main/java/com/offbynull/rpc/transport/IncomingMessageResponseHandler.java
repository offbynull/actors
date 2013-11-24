package com.offbynull.rpc.transport;

/**
 * A handler that should be triggered once a response is ready for an incoming message.
 * @author Kasra F
 */
public interface IncomingMessageResponseHandler {
    /**
     * Indicates that a response is ready to go out. Implementations of this method must not block.
     * @param response response to send out
     * @throws NullPointerException if any arguments are {@code null}
     */
    void responseReady(OutgoingResponse response);
    /**
     * Indicates that a response shouldn't be sent out. Implementations of this method must not block.
     */
    void terminate();
}
