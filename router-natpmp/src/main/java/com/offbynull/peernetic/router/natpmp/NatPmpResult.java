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
package com.offbynull.peernetic.router.natpmp;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

abstract class NatPmpResult {
    private int version;
    private int op;
    private int resultCode;
    
    NatPmpResult(ByteBuffer buffer) {
        Validate.notNull(buffer);
        
        version = buffer.get() & 0xFF;
        op = buffer.get() & 0xFF;
        resultCode = buffer.getShort() & 0xFFFF;
    }

    public int getVersion() {
        return version;
    }

    public int getOp() {
        return op;
    }

    public int getResultCode() {
        return resultCode;
    }
    
}
