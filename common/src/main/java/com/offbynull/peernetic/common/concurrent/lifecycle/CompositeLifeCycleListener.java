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
package com.offbynull.peernetic.common.concurrent.lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * A {@link LifeCycleListener} listener that triggers other {@link LifeCycleListener}s.
 * @author Kasra Faghihi
 */
public final class CompositeLifeCycleListener implements LifeCycleListener {

    private List<LifeCycleListener> listeners;
    
    /**
     * Constructs a {@link CompositeLifeCycleListener} object.
     * @param listeners listeners to trigger
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public CompositeLifeCycleListener(LifeCycleListener ... listeners) {
        Validate.noNullElements(listeners);
        
        this.listeners = new ArrayList<>(Arrays.asList(listeners));
    }
    
    @Override
    public void stateChanged(LifeCycle service, LifeCycleState state) {
        for (LifeCycleListener listener : listeners) {
            try {
                listener.stateChanged(service, state);
            } catch (RuntimeException re) { // NOPMD
                // do nothing
            }
        }
    }
    
}
