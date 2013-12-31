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
 * State of an executing {@link LifeCycle}.
 * @author Kasra Faghihi
 */
public enum LifeCycleState {

    /**
     * {@link LifeCycle#onStart(java.lang.Object...) } is being called.
     */
    STARTING,
    /**
     * {@link LifeCycle#onProcess() } is being called.
     */
    PROCESSING,
    /**
     * {@link LifeCycle#onStop() } is being called.
     */
    STOPPING,
    /**
     * Completed state. Set after {@link #STOPPING}.
     */
    FINISHED,
    /**
     * If {@link LifeCycle#onStart(java.lang.Object...) } or {@link LifeCycle#onStop() } threw an exception. Always moves on to
     * {@link #STOPPING}.
     */
    FAILED

}
