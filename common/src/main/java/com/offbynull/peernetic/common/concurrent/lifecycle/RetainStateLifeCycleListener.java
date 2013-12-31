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

/**
 * A {@link LifeCycleListener} that provides a mechanism to grab the last state notification.
 * @author Kasra Faghihi
 */
public final class RetainStateLifeCycleListener implements LifeCycleListener {
    private volatile LifeCycleState state;
    private volatile boolean failed;

    @Override
    public void stateChanged(LifeCycle service, LifeCycleState state) {
        if (state == LifeCycleState.FAILED) {
            failed = true;
        }
        this.state = state;
    }
    
    /**
     * Get the last state notification.
     * @return last state notification, or {@code null} if no notifications received.
     */
    public LifeCycleState getState() {
        return state;
    }
    
    /**
     * Checks to see if {@link LifeCycleState#FAILED} was encountered.
     * @return {@code true} if {@link LifeCycleState#FAILED} was received, {@code false} otherwise
     */
    public boolean isFailed() {
        return failed;
    }
}
