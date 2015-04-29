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
package com.offbynull.peernetic.core.gateways.recorder;

import java.io.Serializable;
import org.apache.commons.lang3.Validate;

// public because other tools may want to read out recorded data
public final class RecordedMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String srcAddress;
    private String dstSuffix;
    private Object message;

    public RecordedMessage(String srcAddress, String dstSuffix, Object message) {
        Validate.notNull(srcAddress);
        Validate.notNull(dstSuffix);
        Validate.notNull(message);
        this.srcAddress = srcAddress;
        this.dstSuffix = dstSuffix;
        this.message = message;
    }
    
    public String getSrcAddress() {
        return srcAddress;
    }

    public String getDstSuffix() {
        return dstSuffix;
    }

    public Object getMessage() {
        return message;
    }
    
}
