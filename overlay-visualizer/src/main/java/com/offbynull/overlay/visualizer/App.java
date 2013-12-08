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
        visualizer.step("Hello world!",
                new AddNodeCommand<>(1),
                new ChangeNodeCommand(1, null, new Point(random.nextInt(400), random.nextInt(400)), Color.RED),
                new AddNodeCommand<>(2),
                new ChangeNodeCommand(1, null, new Point(random.nextInt(400), random.nextInt(400)), Color.BLUE),
                new AddEdgeCommand<>(1, 2));
//        
//        visualizer.addConnection(1, 7);
//        visualizer.setNodeColor(6, Color.GRAY);
//        visualizer.addNode(9);
//        visualizer.addNode(10);
    }
}
