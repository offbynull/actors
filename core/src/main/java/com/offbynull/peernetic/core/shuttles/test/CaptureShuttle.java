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
package com.offbynull.peernetic.core.shuttles.test;

import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;

public final class CaptureShuttle implements Shuttle {

    private final String prefix;
    private final LinkedBlockingQueue<Message> queuedMessages;

    public CaptureShuttle(String prefix) {
        Validate.notNull(prefix);
        this.prefix = prefix;
        this.queuedMessages = new LinkedBlockingQueue<>();
    }
    
    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        
        queuedMessages.addAll(messages);
    }
    
    public List<Message> drainMessages() {
        List<Message> ret = new ArrayList<>();
        queuedMessages.drainTo(ret);
        return ret;
    }

    public Message takeNextMessage() throws InterruptedException{
        return queuedMessages.take();
    }
}
