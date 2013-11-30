package com.offbynull.rpc.transport;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * A composite {@link OutgoingFilter}.
 * @author Kasra F
 * @param <A> address type
 */
public final class CompositeOutgoingFilter<A> implements OutgoingFilter<A> {
    private List<OutgoingFilter<A>> filters;

    /**
     * Constructs a {@link CompositeOutgoingFilter}.
     * @param filters filter chain
     */
    public CompositeOutgoingFilter(List<OutgoingFilter<A>> filters) {
        Validate.noNullElements(filters);
        
        this.filters = new ArrayList<>(filters);
    }

    @Override
    public ByteBuffer filter(A to, ByteBuffer buffer) {
        ByteBuffer ret = buffer;
        for (OutgoingFilter<A> filter : filters) {
            ret = filter.filter(to, ret);
        }

        return ret;
    }
    
}
