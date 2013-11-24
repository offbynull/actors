package com.offbynull.rpc.transport.fake;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class PerfectLine<A> implements Line<A> {

    @Override
    public List<Packet<A>> queue(A from, A to, ByteBuffer data) {
        return new ArrayList<>(Arrays.asList(new Packet<>(from, to, data, 0L)));
    }

    @Override
    public void unqueue(Collection<Packet<A>> packets) {
    }
    
}
