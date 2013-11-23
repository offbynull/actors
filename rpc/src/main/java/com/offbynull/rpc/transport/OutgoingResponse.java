package com.offbynull.rpc.transport;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class OutgoingResponse {
    private ByteBuffer data;

    public OutgoingResponse(byte[] data) {
        Validate.notNull(data);
        
        this.data = ByteBuffer.allocate(data.length).put(data);
        this.data.flip();
    }

    public ByteBuffer getData() {
        return data.asReadOnlyBuffer();
    }
    
}
