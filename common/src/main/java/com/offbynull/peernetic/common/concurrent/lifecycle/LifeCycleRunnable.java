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

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.Validate;

/**
 * Wraps a {@link LifeCycle} instance as a {@link Runnable}.
 * @author Kasra Faghihi
 */
public final class LifeCycleRunnable implements Runnable {

    private AtomicBoolean consumed;
    private LifeCycle service;
    private LifeCycleListener listener;

    /**
     * Constructs a {@link LifeCycleRunnable} object.
     * @param service life cycle instance to wrap
     * @param listener listener to notify of state changes (can be {@code null})
     * @throws NullPointerException if any argument other than {@code listener} is {@code null}
     */
    public LifeCycleRunnable(LifeCycle service, LifeCycleListener listener) {
        Validate.notNull(service);

        this.service = service;
        this.listener = listener;
        consumed = new AtomicBoolean();
    }

    @Override
    public void run() {
        if (consumed.getAndSet(true)) {
            throw new IllegalStateException();
        }

        try {
            if (listener != null) {
                listener.stateChanged(service, LifeCycleState.STARTING);
            }
            service.onStart();

            if (listener != null) {
                listener.stateChanged(service, LifeCycleState.PROCESSING);
            }
            service.onProcess();
        } catch (Exception t) {
            if (listener != null) {
                listener.stateChanged(service, LifeCycleState.FAILED);
            }
            throw new RuntimeException("Service error", t);
        } finally {
            try {
                if (listener != null) {
                    listener.stateChanged(service, LifeCycleState.STOPPING);
                }
                service.onStop();
            } catch (Exception t) { // NOPMD
                // do nothing
            }
            
            if (listener != null) {
                listener.stateChanged(service, LifeCycleState.FINISHED);
            }
        }
    }
}
