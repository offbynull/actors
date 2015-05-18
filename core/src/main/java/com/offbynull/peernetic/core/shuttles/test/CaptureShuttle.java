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

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A shuttle implementation that captures messages sent to it. Captured messages can be read out using {@link #takeNextMessage() } and
 * {@link #drainMessages() }.
 * @author Kasra Faghihi
 */
public final class CaptureShuttle implements Shuttle {
    private static final Logger LOG = LoggerFactory.getLogger(CaptureShuttle.class);

    private final String prefix;
    private final LinkedBlockingQueue<Message> queuedMessages;

    /**
     * Constructs a {@link CaptureShuttle} instance.
     * @param prefix address prefix of this shuttle
     * @throws NullPointerException if any argument is {@code null}
     */
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
        
        LOG.debug("Received {} messages", messages.size());
        List<Message> filteredMessages = new ArrayList<>(messages.size());
        messages.stream().forEach(x -> {
            try {
                Address dst = x.getDestinationAddress();
                String dstPrefix = dst.getElement(0);
                Validate.isTrue(dstPrefix.equals(prefix));
                
                filteredMessages.add(x);
            } catch (Exception e) {
                LOG.error("Error capturing message: " + x, e);
            }
        });
        
        queuedMessages.addAll(filteredMessages);
    }
    
    /**
     * Removes all captures messages and returns them as a list. This method does not block.
     * @return all captures messages
     */
    public List<Message> drainMessages() {
        List<Message> ret = new ArrayList<>();
        queuedMessages.drainTo(ret);
        return ret;
    }

    /**
     * Removes and returns the next queued captured message, blocking until one becomes available if there are none.
     * @return next captured message
     * @throws InterruptedException if thread interrupted
     */
    public Message takeNextMessage() throws InterruptedException {
        return queuedMessages.take();
    }
}
