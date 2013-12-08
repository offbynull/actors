package com.offbynull.overlay.visualizer;
/**
 * Provides a mechanism to quickly visualize an overlay by placing nodes/connections in a Swing component. A quick example.
 * 
 * <pre>
 * package com.offbynull.overlay.visualizer;
 * 
 * import java.awt.Color;
 * import java.awt.Rectangle;
 * 
 * public class App {
 * 
 *     public static void main(String[] args) {
 *         VisualizeComponent<Integer> component = new VisualizeComponent<>();
 *         
 *         component.addNode(5, 50, 50);
 *         component.addNode(6, 100, 100);
 *         component.addNode(1, 10, 10);
 *         component.resizeNode(1, 100, 200);
 *         component.scaleNode(1, 0.1);
 *         component.addConnection(5, 6);
 *         component.setNodeColor(5, Color.RED);
 *         
 *         VisualizeUtils.displayInWindow("Test", component);
 *     }
 * }
 * </pre>
 */