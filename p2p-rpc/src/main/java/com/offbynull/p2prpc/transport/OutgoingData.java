package com.offbynull.p2prpc.transport;

import java.nio.ByteBuffer;

public final class OutgoingData<A> {
    private A to;
    private ByteBuffer data;

    public OutgoingData(A to, byte[] data) {
        this.to = to;
        this.data = ByteBuffer.allocate(data.length).put(data);
        this.data.flip();
    }

    public A getTo() {
        return to;
    }

    public ByteBuffer getData() {
        return data.asReadOnlyBuffer();
    }
    
}
