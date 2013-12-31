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
 * Listens for changes in the execution of a {@link LifeCycle} instance.
 * @author Kasra Faghihi
 */
public interface LifeCycleListener {
    /**
     * Called each time the execution state changes.
     * @param service instance that had its execution state changed
     * @param state new execution state
     */
    void stateChanged(LifeCycle service, LifeCycleState state);
}