/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
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
