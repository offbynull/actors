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
package com.offbynull.peernetic.common.concurrent.actor;

/**
 * Third-party notification mechanism for {@link ActorQueue}.
 * @author Kasra Faghihi
 */
public interface ActorQueueNotifier {
    /**
     * Blocks for {@code timeout} milliseconds, or until {@link #wakeup() } is invoked. See {@link #wakeup() } for further information.
     * @param timeout maximum amount of time to block
     * @throws InterruptedException if thread was interrupted
     */
    void await(long timeout) throws InterruptedException;
    /**
     * Causes the first {@link #await(long) } invocation that has not yet returned to return immediately.
     * <p/>
     * If another thread is currently blocked in an invocation of {@link #await(long) } then that invocation will return immediately. If no
     * {@link #await(long) } is not in progress then the next invocation of one of these methods will return immediately. Subsequent
     * invocations of {@link #await(long) } will continue to wait.
     */
    void wakeup();
}
