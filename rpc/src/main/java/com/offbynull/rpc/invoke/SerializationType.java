package com.offbynull.rpc.invoke;

/**
 * Type of serialized invokation data.
 * @author Kasra F
 */
public enum SerializationType {

    /**
     * Method invokation data.
     */
    METHOD_CALL,
    /**
     * Method result data.
     */
    METHOD_RETURN,
    /**
     * Method exception data.
     */
    METHOD_THROW,
}
