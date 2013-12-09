package com.offbynull.overlay.visualizer;

import java.awt.Color;
import java.awt.Point;
import java.util.Random;

public class App {

    public static void main(String[] args) {
        JGraphXVisualizer<Integer> visualizer = new JGraphXVisualizer<>();

//        visualizer.addNode(5);
//        visualizer.addNode(6);
//        visualizer.addNode(1);
//        visualizer.addNode(7);
//        visualizer.resizeNode(1, 100, 100);
//        visualizer.scaleNode(1, 0.3);
//        visualizer.addConnection(5, 6);
//        visualizer.setNodeColor(5, Color.RED);
//        
        
        Random random = new Random();
        
        visualizer.visualize();
        visualizer.step("Adding nodes 1 and 2",
                new AddNodeCommand<>(1),
                new ChangeNodeCommand(1, null, new Point(random.nextInt(400), random.nextInt(400)), Color.RED),
                new AddNodeCommand<>(2),
                new ChangeNodeCommand(2, null, new Point(random.nextInt(400), random.nextInt(400)), Color.BLUE));
        
        visualizer.step("Adding nodes 3 and 4",
                new AddNodeCommand<>(3),
                new ChangeNodeCommand(3, null, new Point(random.nextInt(400), random.nextInt(400)), Color.ORANGE),
                new AddNodeCommand<>(4),
                new ChangeNodeCommand(4, null, new Point(random.nextInt(400), random.nextInt(400)), Color.PINK));

        visualizer.step("Connecting 1/2/3 to 4",
                new AddEdgeCommand<>(1, 4),
                new AddEdgeCommand<>(2, 4),
                new AddEdgeCommand<>(3, 4));
        
        visualizer.step("Adding trigger to 4 when no more edges",
                new TriggerOnLingeringNodeCommand(4, new RemoveNodeCommand<>(4)));
        
        visualizer.step("Removing connections from 1 and 2",
                new RemoveEdgeCommand<>(1, 4),
                new RemoveEdgeCommand<>(2, 4));
        
        visualizer.step("Removing connections from 3",
                new RemoveEdgeCommand<>(3, 4));
        
//        
//        visualizer.addConnection(1, 7);
//        visualizer.setNodeColor(6, Color.GRAY);
//        visualizer.addNode(9);
//        visualizer.addNode(10);
    }
}
