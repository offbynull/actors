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

import com.offbynull.peernetic.common.concurrent.actor.ActorQueueWriter;
import org.apache.commons.lang3.Validate;

final class ActivateEndpointCommand<A> {
    private A address;
    private ActorQueueWriter writer;

    public ActivateEndpointCommand(A address, ActorQueueWriter writer) {
        Validate.notNull(address);
        Validate.notNull(writer);
        
        this.writer = writer;
        this.address = address;
    }

    public A getAddress() {
        return address;
    }

    public ActorQueueWriter getWriter() {
        return writer;
    }
    
}
