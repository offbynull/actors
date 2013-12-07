package com.offbynull.rpccommon.filters.selfblock;

import com.offbynull.rpc.transport.OutgoingFilter;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class SelfBlockOutgoingFilter<A> implements OutgoingFilter<A> {
    private SelfBlockId id;

    public SelfBlockOutgoingFilter(SelfBlockId id) {
        Validate.notNull(id);
        
        this.id = id;
    }
    
    @Override
    public ByteBuffer filter(A to, ByteBuffer buffer) {
        ByteBuffer idBuffer = id.getBuffer();
        ByteBuffer revisedBuffer = ByteBuffer.allocate(idBuffer.remaining() + buffer.remaining());
        revisedBuffer.put(idBuffer);
        revisedBuffer.put(buffer);
        revisedBuffer.flip();
        
        return revisedBuffer;
    }
    
}
