package com.offbynull.peernetic.rpc.transport.filters.nil;

import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import java.nio.ByteBuffer;

public class NullIncomingFilter<A> implements IncomingFilter<A> {

    @Override
    public ByteBuffer filter(A from, ByteBuffer buffer) {
        return buffer;
    }
    
}
