package com.offbynull.rpc.transport;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * Outgoing message.
 * @author Kasra F
 * @param <A> address type
 */
public final class OutgoingMessage<A> {
    private A to;
    private ByteBuffer data;

    /**
     * Constructs a {@link OutgoingMessage}.
     * @param to destination address
     * @param data message data
     * @throws NullPointerException if any arguments are {@code null}
     */
    public OutgoingMessage(A to, ByteBuffer data) {
        Validate.notNull(to);
        Validate.notNull(data);
        
        this.to = to;
        this.data = ByteBuffer.allocate(data.remaining()).put(data);
        this.data.flip();
    }

    /**
     * Constructs a {@link OutgoingMessage}.
     * @param to destination address
     * @param data message data
     * @throws NullPointerException if any arguments are {@code null}
     */
    public OutgoingMessage(A to, byte[] data) {
        Validate.notNull(to);
        Validate.notNull(data);
        
        this.to = to;
        this.data = ByteBuffer.allocate(data.length).put(data);
        this.data.flip();
    }

    /**
     * Get destination address.
     * @return destination address
     */
    public A getTo() {
        return to;
    }

    /**
     * Gets a read-only view of the message data;
     * @return message data
     */
    public ByteBuffer getData() {
        return data.asReadOnlyBuffer();
    }
    
}
