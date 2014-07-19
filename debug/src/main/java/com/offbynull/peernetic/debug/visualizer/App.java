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

// CHECKSTYLE:OFF
package com.offbynull.peernetic.debug.visualizer;

import java.awt.Color;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.util.Random;
import org.apache.commons.io.output.ByteArrayOutputStream;

public class App {

    public static void main(String[] args) throws Throwable {
        Random random = new Random();
        
        
        JGraphXVisualizer<Integer> visualizer = new JGraphXVisualizer<>();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Recorder<Integer> recorder = new XStreamRecorder(baos);
        
        visualizer.visualize(recorder, null);
        
        visualizer.step("Adding nodes 1 and 2",
                new AddNodeCommand<>(1),
                new ChangeNodeCommand(1, null, new Point(random.nextInt(400), random.nextInt(400)), Color.RED),
                new AddNodeCommand<>(2),
                new ChangeNodeCommand(2, null, new Point(random.nextInt(400), random.nextInt(400)), Color.BLUE));
        Thread.sleep(500);
        
        visualizer.step("Adding nodes 3 and 4",
                new AddNodeCommand<>(3),
                new ChangeNodeCommand(3, null, new Point(random.nextInt(400), random.nextInt(400)), Color.ORANGE),
                new AddNodeCommand<>(4),
                new ChangeNodeCommand(4, null, new Point(random.nextInt(400), random.nextInt(400)), Color.PINK));
        Thread.sleep(500);

        visualizer.step("Connecting 1/2/3 to 4",
                new AddEdgeCommand<>(1, 4),
                new AddEdgeCommand<>(2, 4),
                new AddEdgeCommand<>(3, 4));
        Thread.sleep(500);
        
        visualizer.step("Adding trigger to 4 when no more edges",
                new TriggerOnLingeringNodeCommand(4, new RemoveNodeCommand<>(4)));
        Thread.sleep(500);
        
        visualizer.step("Removing connections from 1 and 2",
                new RemoveEdgeCommand<>(1, 4),
                new RemoveEdgeCommand<>(2, 4));
        Thread.sleep(500);
        
        visualizer.step("Removing connections from 3",
                new RemoveEdgeCommand<>(3, 4));
        Thread.sleep(500);
        
        recorder.close();
        
        
        
        Thread.sleep(2000);
        
        
        JGraphXVisualizer<Integer> visualizer2 = new JGraphXVisualizer<>();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Player<Integer> player = new XStreamPlayer<>(bais);
        
        visualizer2.visualize();
        
        player.play(visualizer2);
        
    }
}