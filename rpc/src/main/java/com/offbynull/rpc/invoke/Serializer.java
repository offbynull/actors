package com.offbynull.rpc.invoke;

/**
 * Interface for serializing method invokations and invokation results.
 * @author Kasra F
 */
public interface Serializer {
    /**
     * Serialize method invokation.
     * @param invokeData invokation data
     * @return serialized method invokation
     * @throws NullPointerException if any arguments are {@code null}
     */
    byte[] serializeMethodCall(InvokeData invokeData);
    /**
     * Serialize method return value.
     * @param ret return value (may be {@code null})
     * @return serialized return value
     */
    byte[] serializeMethodReturn(Object ret);
    /**
     * Serialize method thrown exception.
     * @param err exception
     * @return serialized exception
     * @throws NullPointerException if any arguments are {@code null}
     */
    byte[] serializeMethodThrow(Throwable err);
}
