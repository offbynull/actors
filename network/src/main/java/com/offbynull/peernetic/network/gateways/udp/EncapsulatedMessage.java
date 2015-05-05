/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.network.gateways.udp;

import java.io.Serializable;
import org.apache.commons.lang3.Validate;

final class EncapsulatedMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String dstSuffix;
    private final Object object;

    public EncapsulatedMessage(String dstSuffix, Object object) {
        Validate.notNull(object);
        
        this.dstSuffix = dstSuffix; // this can be null
        this.object = object; // this technically shouldn't be null
    }

    public String getAddressSuffix() {
        return dstSuffix;
    }

    public Object getObject() {
        // since this is an object that's sent by UDP transport, we should check here as well, because someone could easily serialize this
        // field to be null
        Validate.notNull(object);
        return object;
    }
    
}
