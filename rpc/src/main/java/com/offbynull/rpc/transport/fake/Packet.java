package com.offbynull.rpc.transport.fake;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class Packet<A> {
    private A from;
    private A to;
    private ByteBuffer data;
    private long arriveTime;

    public Packet(A from, A to, ByteBuffer data, long arriveTime) {
        Validate.notNull(from);
        Validate.notNull(to);
        Validate.notNull(data);
        this.from = from;
        this.to = to;
        this.data = ByteBuffer.allocate(data.remaining());
        this.data.put(data);
        this.data.flip();
        this.arriveTime = arriveTime;
    }

    public A getFrom() {
        return from;
    }

    public A getTo() {
        return to;
    }

    public ByteBuffer getData() {
        return data;
    }

    public long getArriveTime() {
        return arriveTime;
    }
    
}
