package com.offbynull.peernetic.rpc.transport.transports.test;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

final class ReceiveMessageEvent<A> {
    private A from;
    private A to;
    private ByteBuffer data;

    public ReceiveMessageEvent(A from, A to, ByteBuffer data) {
        Validate.notNull(from);
        Validate.notNull(to);
        Validate.notNull(data);
        
        this.from = from;
        this.to = to;
        this.data = data;
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
    
}
