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
package com.offbynull.peernetic.common.concurrent.pumps.queue;

import com.offbynull.peernetic.common.concurrent.pump.Message;
import com.offbynull.peernetic.common.concurrent.pump.Pump;
import com.offbynull.peernetic.common.concurrent.pump.PumpReader;
import com.offbynull.peernetic.common.concurrent.pump.PumpWriter;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A message pump implementation based on local queues.
 * @author Kasra Faghihi
 */
public final class QueuePump implements Pump {
    private LinkedBlockingQueue<Iterator<Message>> internalQueue = new LinkedBlockingQueue<>();
    private QueuePumpReader reader = new QueuePumpReader(internalQueue);
    private QueuePumpWriter writer = new QueuePumpWriter(internalQueue);
    
    @Override
    public PumpReader getPumpReader() {
        return reader;
    }
    
    @Override
    public PumpWriter getPumpWriter() {
        return writer;
    }
}
