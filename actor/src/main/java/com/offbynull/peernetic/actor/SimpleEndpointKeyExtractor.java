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
package com.offbynull.peernetic.actor;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * A {@link EndpointKeyExtractor} implementation backed by a {@link Map}.
 * @author Kasra Faghihi
 * @param <K> key type
 */
public final class SimpleEndpointKeyExtractor<K> implements EndpointKeyExtractor<K> {

    private Map<Endpoint, K> map;
    
    /**
     * Constructs a {@link SimpleEndpointKeyExtractor} object.
     * @param map map to use
     * @throws NullPointerException if {@code map} is null / contains {@code null} keys / contains {@code null} values
     */
    public SimpleEndpointKeyExtractor(Map<Endpoint, K> map) {
        Validate.noNullElements(map.keySet());
        Validate.noNullElements(map.values());

        this.map = new HashMap<>(map);
    }
    
    @Override
    public K findKey(Endpoint endpoint) {
        return map.get(endpoint);
    }
    
}
