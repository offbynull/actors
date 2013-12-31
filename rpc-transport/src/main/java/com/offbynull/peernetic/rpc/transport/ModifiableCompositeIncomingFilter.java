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
package com.offbynull.peernetic.rpc.transport;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.lang3.Validate;

/**
 * A composite {@link IncomingFilter} that allows adding/removing of inner filters.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class ModifiableCompositeIncomingFilter<A> implements IncomingFilter<A> {
    private CopyOnWriteArrayList<IncomingFilter<A>> filters;

    /**
     * Construct an empty {@link ModifiableCompositeIncomingFilter}.
     */
    public ModifiableCompositeIncomingFilter() {
        this(Collections.<IncomingFilter<A>>emptyList());
    }
    
    /**
     * Construct a {@link ModifiableCompositeIncomingFilter} populated with {@code filters}.
     * @param filters initial listeners
     */
    public ModifiableCompositeIncomingFilter(Collection<IncomingFilter<A>> filters) {
        Validate.noNullElements(filters);
        
        this.filters = new CopyOnWriteArrayList<>(filters);
    }

    /**
     * Add filters to the start of the chain.
     * @param e filters to add
     * @throws NullPointerException if any element of {@code e} is {@code null}
     */
    public void addFirst(IncomingFilter<A> ... e) {
        Validate.noNullElements(e);
        
        filters.addAll(0, Arrays.asList(e));
    }

    /**
     * Add filters to the end of the chain.
     * @param e filters to add
     * @throws NullPointerException if any element of {@code e} is {@code null}
     */
    public void addLast(IncomingFilter<A> ... e) {
        Validate.noNullElements(e);
        
        filters.addAll(Arrays.asList(e));
    }
    
    /**
     * Remove filters from the chain.
     * @param e filters remove
     * @throws NullPointerException if any element of {@code e} is {@code null}
     */
    public void remove(IncomingFilter<A> ... e) {
        Validate.noNullElements(e);
        
        filters.removeAll(Arrays.asList(e));
    }

    @Override
    public ByteBuffer filter(A from, ByteBuffer buffer) {
        ByteBuffer ret = buffer;
        for (IncomingFilter<A> filter : filters) {
            filter.filter(from, ret);
        }
        return ret;
    }
    
}