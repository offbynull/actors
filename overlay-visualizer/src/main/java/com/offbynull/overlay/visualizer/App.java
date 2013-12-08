package com.offbynull.overlay.visualizer;

import java.awt.Color;

public class App {

    public static void main(String[] args) {
        VisualizeComponent<Integer> component = new VisualizeComponent<>();

//        component.addNode(0);
//        component.removeNode(0);
        
        VisualizeUtils.displayInWindow("Test", component);

        component.addNode(5);
        component.addNode(6);
        component.addNode(1);
        component.addNode(7);
        component.resizeNode(1, 100, 100);
        component.scaleNode(1, 0.3);
        component.addConnection(5, 6);
        component.setNodeColor(5, Color.RED);
    }
}
