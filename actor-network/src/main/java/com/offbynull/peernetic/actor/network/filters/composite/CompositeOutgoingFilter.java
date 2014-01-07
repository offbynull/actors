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
package com.offbynull.peernetic.actor.network.filters.composite;

import com.offbynull.peernetic.actor.network.OutgoingFilter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * A composite {@link OutgoingFilter}.
 * @author Kasra Faghihi
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
