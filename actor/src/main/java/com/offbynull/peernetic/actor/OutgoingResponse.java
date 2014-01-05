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

import org.apache.commons.lang3.Validate;

public final class OutgoingResponse implements Outgoing {
    private Object id; // id must be non-null
    private Endpoint destination;
    private Object content;

    OutgoingResponse(Object id, Endpoint destination, Object content) {
        Validate.notNull(id);
        Validate.notNull(destination);
        Validate.notNull(content);
        
        this.id = id;
        this.destination = destination;
        this.content = content;
    }

    Object getId() {
        return id;
    }

    public Endpoint getDestination() {
        return destination;
    }

    public Object getContent() {
        return content;
    }
    
}
