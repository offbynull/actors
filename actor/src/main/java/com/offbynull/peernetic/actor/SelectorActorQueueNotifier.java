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
package com.offbynull.peernetic.actor;

import java.io.IOException;
import java.nio.channels.Selector;
import org.apache.commons.lang3.Validate;

/**
 * An {@link ActorQueueNotifier} that uses a {@link Selector} to wakeup and notify. That is, in addition to waking up on manual triggers of
 * {@link #wakeup() }, this implementation wakes up when the {@link Selector} has operations ready.
 * @author Kasra Faghihi
 */
public final class SelectorActorQueueNotifier implements ActorQueueNotifier {
    private Selector selector;

    /**
     * Construct a {@link SelectorActorQueueNotifier} object.
     * @param selector selector
     * @throws NullPointerException if any arguments are {@code null}
     */
    public SelectorActorQueueNotifier(Selector selector) {
        Validate.notNull(selector);
        this.selector = selector;
    }

    @Override
    public void await(long timeout) throws InterruptedException {
        try {
            if (timeout == 0L) {
                selector.selectNow();
            } else {
                selector.select(timeout);
            }
            
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    @Override
    public void wakeup() {
        selector.wakeup();
    }
    
}
