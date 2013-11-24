package com.offbynull.rpc.invoke;

/**
 * A listener that gets triggered once a method invoked through {@link Invoker} has finished.
 * @author Kasra F
 */
public interface InvokerListener {
    /**
     * Indicates that the invokation failed.
     * @param t exception
     * @throws NullPointerException if any arguments are {@code null}
     */
    void invokationFailed(Throwable t);
    /**
     * Indicates that the invokation was successful.
     * @param data serialized result or throwable
     * @throws NullPointerException if any arguments are {@code null}
     */
    void invokationFinised(byte[] data);
}
