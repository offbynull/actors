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
package com.offbynull.peernetic.rpc.transport.filters.nil;

import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import java.nio.ByteBuffer;

/**
 * Filter that does nothing.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class NullOutgoingFilter<A> implements OutgoingFilter<A> {

    @Override
    public ByteBuffer filter(A to, ByteBuffer buffer) {
        return buffer;
    }
    
}
