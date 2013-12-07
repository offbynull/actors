package com.offbynull.rpccommon.filters.selfblock;

import com.offbynull.rpc.transport.IncomingFilter;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class SelfBlockIncomingFilter<A> implements IncomingFilter<A> {
    private SelfBlockId id;

    public SelfBlockIncomingFilter(SelfBlockId id) {
        Validate.notNull(id);
        
        this.id = id;
    }

    @Override
    public ByteBuffer filter(A from, ByteBuffer buffer) {
        ByteBuffer incomingIdBuffer = buffer.asReadOnlyBuffer();
        incomingIdBuffer.limit(SelfBlockId.LENGTH);
        
        if (id.getBuffer().equals(incomingIdBuffer)) {
            throw new RuntimeException("Incoming message to self detected");
        }
        
        buffer.position(buffer.position() + SelfBlockId.LENGTH); // adjusted buffer position
        return buffer;
    }
    
}
