package com.offbynull.rpc.invoke;

/**
 * Receives the response from an async proxy method invokation.
 * @author Kasra F
 * @param <T> return type
 */
public interface AsyncResultListener<T> {
    /**
     * The method returned successfully.
     * @param object return object
     */
    void invokationReturned(T object);
    /**
     * The method threw an exception.
     * @param err exception
     * @throws NullPointerException if any arguments are {@code null}.
     */
    void invokationThrew(Throwable err);
    /**
     * Something went wrong in the invokation process
     * @param err error object
     */
    void invokationFailed(Object err);
}
