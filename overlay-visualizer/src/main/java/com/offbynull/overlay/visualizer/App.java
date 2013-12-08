package com.offbynull.overlay.visualizer;

import java.awt.Color;

public class App {

    public static void main(String[] args) {
        VisualizeComponent<Integer> component = new VisualizeComponent<>();
        NodePlacer<Integer> placer = new RandomLocationNodePlacer<>(1000, 1000);
        
        component.addNode(5);
        component.addNode(6);
        component.addNode(1);
        component.resizeNode(1, 100, 200);
        component.scaleNode(1, 0.1);
        component.addConnection(5, 6);
        component.setNodeColor(5, Color.RED);
        
        VisualizeUtils.displayInWindow("Test", component);
    }
}
