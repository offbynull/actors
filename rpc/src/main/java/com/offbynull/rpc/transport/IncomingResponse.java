package com.offbynull.rpc.transport;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * Incoming response.
 * @author Kasra F
 * @param <A> address type
 */
public final class IncomingResponse<A> {
    private A from;
    private ByteBuffer data;
    private long arriveTime;

    /**
     * Constructs an {@link IncomingResponse} object.
     * @param from source address
     * @param data response data
     * @param arriveTime arrival time
     * @throws NullPointerException if any arguments are {@code null}
     */
    public IncomingResponse(A from, ByteBuffer data, long arriveTime) {
        Validate.notNull(from);
        Validate.notNull(data);
        
        this.from = from;
        this.data = ByteBuffer.allocate(data.remaining()).put(data);
        this.arriveTime = arriveTime;
        this.data.flip();
    }

    /**
     * Constructs an {@link IncomingResponse} object.
     * @param from source address
     * @param data response data
     * @param arriveTime arrival time
     * @throws NullPointerException if any arguments are {@code null}
     */
    public IncomingResponse(A from, byte[] data, long arriveTime) {
        Validate.notNull(from);
        Validate.notNull(data);
        
        this.from = from;
        this.data = ByteBuffer.allocate(data.length).put(data);
        this.arriveTime = arriveTime;
        this.data.flip();
    }

    /**
     * Get source address.
     * @return source address
     */
    public A getFrom() {
        return from;
    }

    /**
     * Gets a read-only view of the response data;
     * @return response data
     */
    public ByteBuffer getData() {
        return data.asReadOnlyBuffer();
    }

    /**
     * Gets the arrival time.
     * @return arrival time
     */
    public long getArriveTime() {
        return arriveTime;
    }
    
}
