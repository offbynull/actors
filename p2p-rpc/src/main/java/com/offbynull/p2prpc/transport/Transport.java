package com.offbynull.p2prpc.transport;

import java.io.IOException;

/**
 * An interface to send, receive, and reply to messages over a network. Implementations must be thread-safe.
 * @author Kasra F
 * @param <A> address
 */
public interface Transport<A> {
    /**
     * Starts the transport.
     * @throws IOException on error
     */
    void start() throws IOException;
    /**
     * Stops the transport. Cannot be restarted once stopped.
     * @throws IOException on error
     */
    void stop() throws IOException;
    
    /**
     * Queues a message to be sent out. The behaviour of this method is undefined if the transport isn't in a started state.
     * @param message message contents and recipient
     * @param listener handles message responses
     */
    void sendMessage(OutgoingMessage<A> message, OutgoingMessageResponseListener<A> listener);
    
    /**
     * Adds a listener to listen for and respond to incoming messages.
     * @param listener listener that listens for incoming messages
     */
    void addMessageListener(IncomingMessageListener<A> listener);
    
    /**
     * Removes a listener that listens for and respond to incoming messages.
     * @param listener listener that listens for incoming messages
     */
    void removeMessageListener(IncomingMessageListener<A> listener);
}
