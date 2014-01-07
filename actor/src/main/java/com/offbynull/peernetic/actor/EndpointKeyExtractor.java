package com.offbynull.peernetic.actor;

/**
 * An interface that maps {@link Endpoint}s to keys used by {@link EndpointFinder}. Reverse of {@link EndpointFinder}.
 * @author Kasra Faghihi
 * @param <K> key type
 */
public interface EndpointKeyExtractor<K> {
    /**
     * Find an endpoint by key.
     * @param endpoint endpoint to get key for
     * @return key associated with {@code endpoint}, or {@code null} if no such key could be found
     * @throws NullPointerException if any arguments are {@code null}
     */
    K findKey(Endpoint endpoint);
}
