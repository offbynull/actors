package com.offbynull.rpc.invoke;

/**
 * Interface called by methods of proxy objects generated through
 * {@link Capturer#createInstance(com.offbynull.rpc.invoke.CapturerHandler) }.
 * @author Kasra F
 */
public interface CapturerHandler {
    /**
     * Indicates that a method on a proxy object was called.
     * @param data serialized invokation data
     * @return serialized result data
     * @throws NullPointerException if any argument are {@code null}
     */
    byte[] invokationTriggered(byte[] data);
    /**
     * Indicates that a method invokation on the proxy object failed.
     * @param err error thrown
     * @throws NullPointerException if any argument are {@code null}
     */
    void invokationFailed(Throwable err);
}
