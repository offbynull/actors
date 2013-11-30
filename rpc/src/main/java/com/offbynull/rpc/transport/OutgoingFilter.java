package com.offbynull.rpc.transport;

import java.nio.ByteBuffer;

/**
 * Modifies raw message data leaving a {@link Transport}.
 * @author Kasra F
 * @param <A> address type
 */
public interface OutgoingFilter<A> {
    /**
     * Modifies data.
     * @param to address the data is to
     * @param buffer data to be modified
     * @throws NullPointerException if any arguments are {@code null}
     * @return modified data
     */
    ByteBuffer filter(A to, ByteBuffer buffer); 
}
