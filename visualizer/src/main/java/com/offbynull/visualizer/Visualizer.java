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
package com.offbynull.visualizer;

/**
 * A visualizer.
 * @author Kasra F
 * @param <A> address type
 */
public interface Visualizer<A> {

    /**
     * Performs a list of {@link Command}s.
     * @param output output message for the step
     * @param commands commands to execute
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    void step(String output, Command<A> ... commands);
    /**
     * Starts the visualizer. Equivalent to calling {@link #visualize(com.offbynull.visualizer.Recorder,
     * com.offbynull.visualizer.VisualizerEventListener) } with {@code null} for both parameters.
     */
    void visualize();
    /**
     * Starts the visualizer with a {@link Recorder} and a {@link VisualizerEventListener}.
     * @param recorder step recorder
     * @param listener event listener
     */
    void visualize(Recorder recorder, VisualizerEventListener listener);
    
}
