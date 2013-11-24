package com.offbynull.rpc.transport.fake;

import java.util.Comparator;

final class MessageArriveTimeComparator implements Comparator<Message> {

    @Override
    public int compare(Message o1, Message o2) {
        return Long.compare(o1.getArriveTime(), o2.getArriveTime());
    }
    
}
