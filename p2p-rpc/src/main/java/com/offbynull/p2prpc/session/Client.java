package com.offbynull.p2prpc.session;

import java.io.IOException;

/**
 * Implementations must be thread-safe.
 * @param <A> 
 */
public interface Client<A> {
    byte[] send(A address, byte[] data, long timeout) throws IOException, InterruptedException;
}