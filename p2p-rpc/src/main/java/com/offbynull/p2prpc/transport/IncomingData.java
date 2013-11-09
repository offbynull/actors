package com.offbynull.p2prpc.transport;

import java.nio.ByteBuffer;

public final class IncomingData<A> {
    private A from;
    private ByteBuffer data;
    private long arriveTime;

    public IncomingData(A from, byte[] data, long arriveTime) {
        this.from = from;
        this.data = ByteBuffer.allocate(data.length).put(data);
        this.arriveTime = arriveTime;
        this.data.flip();
    }

    public A getFrom() {
        return from;
    }

    public ByteBuffer getData() {
        return data.asReadOnlyBuffer();
    }

    public long getArriveTime() {
        return arriveTime;
    }
    
}
