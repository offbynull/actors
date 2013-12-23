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
package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.overlay.common.id.Pointer;

/**
 * The result of a routing operation from {@link FingerTable}.
 * @param <A> address type
 * @author Kasra Faghihi
 */
public final class RouteResult<A> {
    private ResultType resultType;
    private Pointer<A> pointer;

    /**
     * Constructs a {@link RouteResult} object.
     * @param resultType result type
     * @param pointer pointer
     * @throws NullPointerException if any arguments are {@code null}
     */
    public RouteResult(ResultType resultType, Pointer<A> pointer) {
        if (resultType == null || pointer == null) {
            throw new NullPointerException();
        }
        this.resultType = resultType;
        this.pointer = pointer;
    }

    /**
     * Get the result type.
     * @return result type
     */
    public ResultType getResultType() {
        return resultType;
    }

    /**
     * Get the pointer.
     * @return pointer
     */
    public Pointer<A> getPointer() {
        return pointer;
    }

    /**
     * Result type.
     */
    public enum ResultType {

        /**
         * Routed to self.
         */
        SELF,
        /**
         * Have direct reference to another node.
         */
        FOUND,
        /**
         * Don't have a direct reference to another node, but do have a reference to get closer to that node.
         */
        CLOSEST_PREDECESSOR
    }
    
}
