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

import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.gateway.Gateway;
import java.io.File;
import org.apache.commons.lang3.Validate;

public final class ReplayerGateway implements Gateway {

    private final Thread readThread;
    
    public static ReplayerGateway replay(Shuttle dstShuttle, String dstAddress, File file, Serializer serializer) {
        Validate.notNull(dstShuttle);
        Validate.notNull(dstAddress);
        Validate.notNull(file);
        Validate.notNull(serializer);
        Validate.isTrue(AddressUtils.isPrefix(dstShuttle.getPrefix(), dstAddress));
        
        ReadRunnable readRunnable = new ReadRunnable(dstShuttle, dstAddress, file, serializer);
        Thread readThread = new Thread(readRunnable);
        readThread.setDaemon(true);
        readThread.setName(RecorderGateway.class.getSimpleName());
        
        ReplayerGateway ret = new ReplayerGateway(readThread);
        
        readThread.start();        
        
        return ret;
    }

    private ReplayerGateway(Thread readThread) {
        this.readThread = readThread;
    }
    
    public void await() throws InterruptedException {
        readThread.join();
    }

    @Override
    public void close() {
        readThread.interrupt();
    }

    
}
