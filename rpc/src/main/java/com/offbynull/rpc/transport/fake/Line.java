package com.offbynull.rpc.transport.fake;

import java.nio.ByteBuffer;

public interface Line<A> {
    LineResult process(A from, A to, ByteBuffer data);
}
