/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.debug.visualizer;

import java.io.Closeable;

/**
 * Records events passed to a {@link Visualizer}.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public interface Recorder<A> extends Closeable {
    /**
     * Record a visualizer step.
     * @param output text output for the step
     * @param commands commands for the step
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    void recordStep(String output, Command<A> ... commands);
}
