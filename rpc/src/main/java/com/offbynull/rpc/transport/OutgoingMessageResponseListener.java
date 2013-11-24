package com.offbynull.rpc.transport;

/**
 * A listener that gets triggered when a response arrives for an {@link OutgoingMessage}. Implementations must be thread-safe.
 * @author Kasra F
 * @param <A> address type
 */
public interface OutgoingMessageResponseListener<A> {
    /**
     * Indicates that a response has arrived. Implementations of this method must not block.
     * @param response response
     * @throws NullPointerException if any arguments are {@code null}
     */
    void responseArrived(IncomingResponse<A> response);
    /**
     * Indicates that an internal error occurred. Implementations of this method must not block.
     * @param error throwable that caused the error (may be {@code null)}
     */
    void internalErrorOccurred(Throwable error);
    /**
     * Indicates that waiting for the response timed out. Implementations of this method must not block.
     */
    void timedOut();
}
