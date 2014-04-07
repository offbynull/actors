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
package com.offbynull.peernetic.actor.network;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointKeyExtractor;
import java.net.SocketAddress;
import org.apache.commons.lang3.Validate;

/**
 * A simple {@link EndpointKeyExtractor} implementation that allows the user to extract the address out of a {@link NetworkEndpoint}.
 * @author Kasra Faghihi
 */
public final class NetworkEndpointKeyExtractor implements EndpointKeyExtractor<SocketAddress> {

    @Override
    public SocketAddress findKey(Endpoint endpoint) {
        Validate.notNull(endpoint);
        if (!(endpoint instanceof NetworkEndpoint)) {
            return null;
        }

        return ((NetworkEndpoint) endpoint).getAddress();
    }
    
}
