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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

final class DefaultCommandResponseListener<T> implements CommandResponseListener<T> {

    private ArrayBlockingQueue<T> queue = new ArrayBlockingQueue<>(1);
    
    @Override
    public void commandResponded(T response) {
        queue.add(response);
    }
    
    public T waitForResponse() {
        try {
            return queue.poll(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }
}
