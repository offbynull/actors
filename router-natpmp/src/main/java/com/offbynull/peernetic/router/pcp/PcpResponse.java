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
package com.offbynull.peernetic.router.pcp;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

abstract class PcpResponse {
    private int op;
    private long lifetime;
    private long epochTime;
    
    PcpResponse(ByteBuffer buffer) {
        Validate.notNull(buffer);
        
        if (buffer.remaining() < 4 || buffer.remaining() > 1100 || buffer.remaining() % 4 != 0) {
            throw new IllegalArgumentException("Bad packet size: " + buffer.remaining());
        }
        
        int version = buffer.get() & 0xFF;
        
        if (version != 2) {
            throw new IllegalArgumentException("Unknown version: " + version);
        }
        
        int temp = buffer.get() & 0xFF;
        
        if ((temp & 128) != 128) {
            throw new IllegalArgumentException("Bad R-flag: " + temp);
        }
        op = temp >> 7; // discard first bit, it was used for rflag
        
        buffer.get(); // skip reserved field
        
        int resultCodeNum = buffer.get() & 0xFF;
        PcpResultCode[] resultCodes = PcpResultCode.values();
        
        if (resultCodeNum >= resultCodes.length) {
            throw new IllegalArgumentException("Unknown result code encountered: " + resultCodeNum);
        } else if (resultCodeNum != PcpResultCode.SUCCESS.ordinal()) {
            PcpResultCode resultCode = resultCodes[resultCodeNum];
            throw new IllegalArgumentException(resultCode.name() + ": " + resultCode.getMessage());
        }
        
        lifetime = buffer.getInt() & 0xFFFFFFFFL;
        
        epochTime = buffer.getInt() & 0xFFFFFFFFL;
        
        for (int i = 0; i < 12; i++) {
            if (buffer.get() != 0) {
                throw new IllegalArgumentException("Reserved space indicates unsuccessful response");
            }
        }
    }

    public int getOp() {
        return op;
    }

    public long getLifetime() {
        return lifetime;
    }

    public long getEpochTime() {
        return epochTime;
    }
    
}
