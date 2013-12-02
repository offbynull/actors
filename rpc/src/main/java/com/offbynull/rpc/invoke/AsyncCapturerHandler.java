package com.offbynull.rpc.invoke;

/**
 * Interface called by methods of async proxy objects generated through
 * {@link AsyncCapturer#createInstance(com.offbynull.rpc.invoke.AsyncCapturerHandler) }.
 * @author Kasra F
 */
public interface AsyncCapturerHandler {
    /**
     * Indicates that a method on an async proxy object was called.
     * @param data serialized invokation data
     * @param responseHandler called once a response is ready
     * @throws NullPointerException if any argument are {@code null}
     */
    void invokationTriggered(byte[] data, AsyncCapturerHandlerCallback responseHandler);
    /**
     * Indicates that a method invokation on an async proxy object failed.
     * @param err error thrown
     * @throws NullPointerException if any argument are {@code null}
     */
    void invokationFailed(Throwable err);
}
