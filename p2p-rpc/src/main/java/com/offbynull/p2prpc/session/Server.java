package com.offbynull.p2prpc.session;

import java.io.IOException;

/**
 * Implementations must be thread-safe.
 * @param <A> 
 */
public interface Server<A> {
    void start(MessageListener<A> callback) throws IOException;
    void stop() throws IOException;
}
