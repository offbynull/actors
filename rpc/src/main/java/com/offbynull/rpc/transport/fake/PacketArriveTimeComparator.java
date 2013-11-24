package com.offbynull.rpc.transport.fake;

import java.util.Comparator;

final class PacketArriveTimeComparator implements Comparator<Packet> {

    @Override
    public int compare(Packet o1, Packet o2) {
        return Long.compare(o1.getArriveTime(), o2.getArriveTime());
    }
    
}
