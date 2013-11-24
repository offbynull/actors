package com.offbynull.rpc.transport.fake;

import java.nio.ByteBuffer;
import java.util.Arrays;

public final class PerfectLine<A> implements Line<A> {

    @Override
    public LineResult<A> process(A from, A to, ByteBuffer data) {
        return new LineResult<>(Arrays.asList(new Packet<>(from, to, data, 0L)));
    }
    
}
