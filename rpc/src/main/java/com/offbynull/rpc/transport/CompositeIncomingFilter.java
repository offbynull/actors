package com.offbynull.rpc.transport;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * A composite {@link IncomingFilter}.
 * @author Kasra F
 * @param <A> address type
 */
public final class CompositeIncomingFilter<A> implements IncomingFilter<A> {
    private List<IncomingFilter<A>> filters;

    /**
     * Constructs a {@link CompositeIncomingFilter}.
     * @param filters filter chain
     */
    public CompositeIncomingFilter(List<IncomingFilter<A>> filters) {
        Validate.noNullElements(filters);
        
        this.filters = new ArrayList<>(filters);
    }

    @Override
    public ByteBuffer filter(A from, ByteBuffer buffer) {
        ByteBuffer ret = buffer;
        for (IncomingFilter<A> filter : filters) {
            ret = filter.filter(from, ret);
        }

        return ret;
    }
    
}
