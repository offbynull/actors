/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.actor;

/**
 * An interface that maps {@link Endpoint}s to keys used by {@link EndpointFinder}. Reverse of {@link EndpointFinder}.
 * @author Kasra Faghihi
 * @param <K> key type
 */
public interface EndpointKeyExtractor<K> {
    /**
     * Find an endpoint by key.
     * @param endpoint endpoint to get key for
     * @return key associated with {@code endpoint}, or {@code null} if no such key could be found
     * @throws NullPointerException if any arguments are {@code null}
     */
    K findKey(Endpoint endpoint);
}
