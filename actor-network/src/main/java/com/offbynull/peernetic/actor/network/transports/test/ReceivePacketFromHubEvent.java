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
package com.offbynull.peernetic.actor.network.transports.test;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

final class ReceivePacketFromHubEvent<A> {
    private A from;
    private A to;
    private ByteBuffer data;

    public ReceivePacketFromHubEvent(A from, A to, ByteBuffer data) {
        Validate.notNull(from);
        Validate.notNull(to);
        Validate.notNull(data);
        
        this.from = from;
        this.to = to;
        this.data = data;
    }

    public A getFrom() {
        return from;
    }

    public A getTo() {
        return to;
    }

    public ByteBuffer getData() {
        return data;
    }
    
}
