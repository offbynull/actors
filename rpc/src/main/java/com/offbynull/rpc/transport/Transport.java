package com.offbynull.rpc.transport;

import java.io.IOException;

/**
 * An interface to send, receive, and reply to messages over a network. Implementations must be thread-safe.
 * @author Kasra F
 * @param <A> address
 */
public interface Transport<A> {
    /**
     * Starts the transport.
     * @param listener listener for incoming messages
     * @throws IOException on error
     * @throws IllegalStateException if already started or stopped
     * @throws NullPointerException if {@code listener == null}
     */
    void start(IncomingMessageListener<A> listener) throws IOException;

    /**
     * Stops the transport. Cannot be restarted once stopped.
     * @throws IOException on error
     * @throws IllegalStateException if not started
     */
    void stop() throws IOException;
    
    /**
     * Queues a message to be sent out. The behaviour of this method is undefined if the transport isn't in a started state.
     * @param message message contents and recipient
     * @param listener handles message responses
     */
    void sendMessage(OutgoingMessage<A> message, OutgoingMessageResponseListener<A> listener);
}
