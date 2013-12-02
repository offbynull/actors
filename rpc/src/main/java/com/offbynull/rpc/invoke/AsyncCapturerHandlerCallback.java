package com.offbynull.rpc.invoke;

/**
 * Interface used to signal that the response for a async proxy call has arrived.
 * @author Kasra F
 */
public interface AsyncCapturerHandlerCallback {
    /**
     * Response arrived.
     * @param response serialized response
     */
    void responseArrived(byte[] response);
    /**
     * Response failed.
     * @param err error
     */
    void responseFailed(Throwable err);
}
