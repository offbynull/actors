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
import com.offbynull.peernetic.common.concurrent.pump.PumpWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;

final class QueuePumpWriter implements PumpWriter {
    private LinkedBlockingQueue<Iterator<Message>> queue;

    QueuePumpWriter(LinkedBlockingQueue<Iterator<Message>> queue) {
        Validate.notNull(queue);

        this.queue = queue;
    }

    @Override
    public void push(Collection<Message> data) {
        Validate.noNullElements(data);
        
        queue.add(new ArrayList<>(data).iterator());
    }

    @Override
    public void push(Message ... data) throws InterruptedException, IOException {
        push(Arrays.asList(data));
    }
}
