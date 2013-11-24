package com.offbynull.rpc.transport;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * Outgoing response.
 * @author Kasra F
 */
public final class OutgoingResponse {
    private ByteBuffer data;

    /**
     * Constructs a {@link OutgoingResponse} object.
     * @param data response data
     * @throws NullPointerException if any arguments are {@code null}
     */
    public OutgoingResponse(byte[] data) {
        Validate.notNull(data);
        
        this.data = ByteBuffer.allocate(data.length).put(data);
        this.data.flip();
    }

    /**
     * Gets a read-only view of the response data;
     * @return response data
     */
    public ByteBuffer getData() {
        return data.asReadOnlyBuffer();
    }
    
}
