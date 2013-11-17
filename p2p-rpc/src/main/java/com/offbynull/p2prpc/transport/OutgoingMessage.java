package com.offbynull.p2prpc.transport;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class OutgoingMessage<A> {
    private A to;
    private ByteBuffer data;

    public OutgoingMessage(A to, byte[] data) {
        Validate.notNull(to);
        Validate.notNull(data);
        
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
