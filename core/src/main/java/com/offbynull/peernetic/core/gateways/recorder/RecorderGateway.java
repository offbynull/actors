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
import com.offbynull.peernetic.core.gateway.InputGateway;
import java.io.File;
import org.apache.commons.lang3.Validate;


public final class RecorderGateway implements InputGateway {
    private final Thread writeThread;
    private final RecorderShuttle recorderShuttle;
    
    public static RecorderGateway record(String prefix, Shuttle dstShuttle, String dstAddress, File file, Serializer serializer) {
        Validate.notNull(prefix);
        Validate.notNull(dstShuttle);
        Validate.notNull(dstAddress);
        Validate.notNull(file);
        Validate.notNull(serializer);
        Validate.isTrue(AddressUtils.isPrefix(dstShuttle.getPrefix(), dstAddress));
        
        WriteRunnable writeRunnable = new WriteRunnable(file, prefix, serializer);
        WriteBus writeBus = writeRunnable.getBus();
        RecorderShuttle recorderShuttle = new RecorderShuttle(prefix, writeBus, dstShuttle, dstAddress);
        
        Thread writeThread = new Thread(writeRunnable);
        writeThread.setDaemon(true);
        writeThread.setName(RecorderGateway.class.getSimpleName());
        
        RecorderGateway ret = new RecorderGateway(writeThread, recorderShuttle);
        
        writeThread.start();        
        
        return ret;
    }

    private RecorderGateway(Thread writeThread, RecorderShuttle recorderShuttle) {
        Validate.notNull(writeThread);
        Validate.notNull(recorderShuttle);

        this.writeThread = writeThread;
        this.recorderShuttle = recorderShuttle;
    }

    
    @Override
    public Shuttle getIncomingShuttle() {
        return recorderShuttle;
    }

    public void await() throws InterruptedException {
        writeThread.join();
    }
    
    @Override
    public void close() {
        writeThread.interrupt(); // thread will close the bus when it gets an exception
    }
    
}
