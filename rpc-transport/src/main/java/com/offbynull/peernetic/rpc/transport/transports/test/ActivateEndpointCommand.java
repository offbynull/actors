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
package com.offbynull.peernetic.rpc.transport.transports.test;

import com.offbynull.peernetic.actor.Endpoint;
import org.apache.commons.lang3.Validate;

final class ActivateEndpointCommand<A> {
    private A address;
    private Endpoint endpoint;

    public ActivateEndpointCommand(A address, Endpoint endpoint) {
        Validate.notNull(address);
        Validate.notNull(endpoint);
        
        this.endpoint = endpoint;
        this.address = address;
    }

    public A getAddress() {
        return address;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }
    
}
