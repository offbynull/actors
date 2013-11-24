package com.offbynull.rpc.transport.fake;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

/**
 * Controls how messages are sent in a hub. For example, depending on the line, a message may be dropped/duplicated/corrupted/slow/fast...
 * @author Kasra F
 * @param <A> address type
 */
public interface Line<A> {
    /**
     * Generate {@link Message} objects for an outgoing message.
     * @param from source
     * @param to destination
     * @param data contents
     * @return list of {@link Message} objects
     */
    List<Message<A>> queue(A from, A to, ByteBuffer data);
    
    /**
     * Signals that {@link Message} objects that were created by this line have arrived.
     * @param messages {@link Message} objects that have arrived
     */
    void unqueue(Collection<Message<A>> messages);
}
