package com.offbynull.rpc.transport;

import java.nio.ByteBuffer;

/**
 * Modifies raw message data coming in to or leaving a {@link Transport}.
 * @author Kasra F
 * @param <A> address type
 */
public interface IncomingFilter<A> {
    /**
     * Modifies data.
     * @param from address the data is to/from
     * @param buffer data to be modified
     * @return modified data
     */
    ByteBuffer filter(A from, ByteBuffer buffer); 
}
