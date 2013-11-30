package com.offbynull.rpc.transport.fake;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A line implementation without any constraints or failures. Takes no time to transmit a message.
 * @author Kasra F
 * @param <A> address type
 */
public final class PerfectLine<A> implements Line<A> {

    @Override
    public List<Message<A>> depart(A from, A to, ByteBuffer data) {
        return new ArrayList<>(Arrays.asList(new Message<>(from, to, data, 0L)));
    }

    @Override
    public void arrive(Collection<Message<A>> packets) {
    }
    
}
