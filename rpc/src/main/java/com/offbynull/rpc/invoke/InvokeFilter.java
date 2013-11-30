package com.offbynull.rpc.invoke;

/**
 * Inspects and potentially makes changes to an invokation.
 * @author Kasra F
 */
public interface InvokeFilter {
    /**
     * Validates/inspects/modifies invokation data.
     * @param data invokation data
     * @throws NullPointerException if any arguments are {@code null}
     * @throws RuntimeException on validation error
     * @return modified invokation data
     */
    InvokeData filter(InvokeData data);
}
