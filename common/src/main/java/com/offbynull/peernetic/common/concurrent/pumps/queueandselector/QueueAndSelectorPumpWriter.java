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
package com.offbynull.peernetic.common.concurrent.pumps.queueandselector;

import com.offbynull.peernetic.common.concurrent.pump.PumpWriter;
import java.io.IOException;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;

final class QueueAndSelectorPumpWriter<T> implements PumpWriter<T> {
    private LinkedBlockingQueue<Iterator<T>> queue;
    private Selector selector;

    QueueAndSelectorPumpWriter(LinkedBlockingQueue<Iterator<T>> queue, Selector selector) {
        Validate.notNull(queue);
        Validate.notNull(selector);

        this.queue = queue;
        this.selector = selector;
    }

    @Override
    public void push(Collection<T> data) {
        Validate.validState(selector.isOpen());
        Validate.noNullElements(data);
        
        queue.add(Collections.unmodifiableList(new ArrayList<>(data)).iterator());
        selector.wakeup();
    }

    @Override
    public void push(T... data) throws InterruptedException, IOException {
        push(Arrays.asList(data));
    }
}
