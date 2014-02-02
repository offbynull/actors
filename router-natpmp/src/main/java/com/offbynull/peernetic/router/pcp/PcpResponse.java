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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Validate;

public abstract class PcpResponse {
    private int op;
    private long lifetime;
    private long epochTime;
    private List<PcpOption> options;
    
    PcpResponse(ByteBuffer buffer) {
        Validate.notNull(buffer);
        
        if (buffer.remaining() < 4 || buffer.remaining() > 1100 || buffer.remaining() % 4 != 0) {
            throw new IllegalArgumentException("Bad packet size: " + buffer.remaining());
        }
        
        int version = buffer.get() & 0xFF;
        Validate.isTrue(version == 2, "Unknown version: %d", version);
        
        int temp = buffer.get() & 0xFF;
        Validate.isTrue((temp & 128) == 128, "Bad R-flag: %d", temp);
        op = temp & 0x7F; // discard first bit, it was used for rflag
        
        buffer.get(); // skip reserved field
        
        int resultCodeNum = buffer.get() & 0xFF;
        PcpResultCode[] resultCodes = PcpResultCode.values();
        
        Validate.isTrue(resultCodeNum < resultCodes.length, "Unknown result code encountered: %d", resultCodeNum);
        Validate.isTrue(resultCodeNum == PcpResultCode.SUCCESS.ordinal(), "Unsuccessful result code: %s [%s]",
                resultCodes[resultCodeNum].toString(), resultCodes[resultCodeNum].getMessage());
        
        lifetime = buffer.getInt() & 0xFFFFFFFFL;
        
        epochTime = buffer.getInt() & 0xFFFFFFFFL;
        
        for (int i = 0; i < 12; i++) {
            Validate.isTrue(buffer.get() == 0, "Reserved space indicates unsuccessful response");
        }
        
        options = Collections.emptyList();
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

    public List<PcpOption> getOptions() {
        return options;
    }
    
    // must be called by child classes's constructor
    protected final void parseOptions(ByteBuffer buffer) {
        List<PcpOption> pcpOptionsList = new ArrayList<>();
        while (buffer.hasRemaining()) {
            PcpOption option;

            try {
                buffer.mark();
                option = new FilterPcpOption(buffer);
                pcpOptionsList.add(option);
                continue;
            } catch (IllegalArgumentException iae) {
                buffer.reset();
            }
            
            try {
                buffer.mark();
                option = new PreferFailurePcpOption(buffer);
                pcpOptionsList.add(option);
                continue;
            } catch (IllegalArgumentException iae) {
                buffer.reset();
            }
            
            try {
                buffer.mark();
                option = new ThirdPartyPcpOption(buffer);
                pcpOptionsList.add(option);
                continue;
            } catch (IllegalArgumentException iae) {
                buffer.reset();
            }
            
            option = new UnknownPcpOption(buffer);
            pcpOptionsList.add(option);
        }
        
        options = Collections.unmodifiableList(pcpOptionsList);
    } 
}
