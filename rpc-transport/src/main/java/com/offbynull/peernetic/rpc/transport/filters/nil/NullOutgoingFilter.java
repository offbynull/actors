package com.offbynull.peernetic.rpc.transport.filters.nil;

import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import java.nio.ByteBuffer;

public final class NullOutgoingFilter<A> implements OutgoingFilter<A> {

    @Override
    public ByteBuffer filter(A to, ByteBuffer buffer) {
        return buffer;
    }
    
}
