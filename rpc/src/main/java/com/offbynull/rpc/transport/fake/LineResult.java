package com.offbynull.rpc.transport.fake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LineResult<A> {
    private List<Packet<A>> packets;

    public LineResult(List<Packet<A>> packets) {
        this.packets = Collections.unmodifiableList(new ArrayList<>(packets));
    }
    
    List<Packet<A>> getPackets() {
        return packets;
    }
}
