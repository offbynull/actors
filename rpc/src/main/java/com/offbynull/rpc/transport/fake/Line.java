package com.offbynull.rpc.transport.fake;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

public interface Line<A> {
    List<Packet<A>> queue(A from, A to, ByteBuffer data);
    void unqueue(Collection<Packet<A>> packets);
}
