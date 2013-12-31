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
 * {@link LifeCycle} interface should be implemented by any class whose instances are to be executed by a thread. Like {@code Runnable}, but
 * provides start-up and shutdown phases.
 * <p/>
 * Classes that implement this interface should expect {@link #onStart(java.lang.Object...) } to get called first,
 * then {@link #onProcess() }, then finally {@link #onStop() }. In the event of exceptions thrown by these methods...
 * <ul>
 * <li>If {@link #onStart(java.lang.Object...) } throws an exception, expect {@link #onProcess() } to be skipped.</li>
 * <li>If {@link #onProcess() } throws an exception, expect to move on to {@link #onStop() }.</li>
 * <li>The executing thread should stop regardless of if {@link #onStop() } throws an exception.</li>
 * </ul>
 * <p/>
 * Implementations should be designed to run a {@link LifeCycle} instance once. That is, subsequent runs of the same {@link LifeCycle}
 * instance aren't expected to work.
 * @author Kasra Faghihi
 */
public interface LifeCycle {
    /**
     * Start-up / initialization.
     * @param init start-up variables
     * @throws Exception on error -- if encountered the next method called will be {@link #onStop() }
     */
    void onStart(Object ... init) throws Exception;
    
    /**
     * Process.
     * @throws Exception on error
     */
    void onProcess() throws Exception;
    
    /**
     * Shutdown.
     * @throws Exception on error
     */
    void onStop() throws Exception;
}
