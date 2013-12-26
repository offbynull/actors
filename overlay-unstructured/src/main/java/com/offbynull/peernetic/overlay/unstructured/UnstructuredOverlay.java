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
package com.offbynull.peernetic.overlay.unstructured;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import org.apache.commons.lang3.Validate;

/**
 * A service that periodically calls {@link LinkManager#process(long) }.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class UnstructuredOverlay<A> extends AbstractExecutionThreadService {
    private LinkManager<A> linkManager;

    /**
     * Constructs a {@link UnstructuredOverlay} object.
     * @param linkManager link manager
     * @throws NullPointerException if any arguments are {@code null}
     */
    public UnstructuredOverlay(LinkManager<A> linkManager) {
        Validate.notNull(linkManager);
        this.linkManager = linkManager;
    }

    @Override
    protected void run() throws Exception {
        while (true) {
            long timestamp = System.currentTimeMillis();
            long nextTimestamp = linkManager.process(timestamp);
            
            Thread.sleep(Math.max(1L, nextTimestamp - timestamp));
        }
    }
    
}
