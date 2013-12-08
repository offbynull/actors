package com.offbynull.overlay.visualizer;
/**
 * Provides a mechanism to quickly visualize an overlay by placing nodes/connections in a Swing component. A quick example.
 * 
 * <pre>
 *        JGraphXVisualizer<Integer> visualizer = new JGraphXVisualizer<>();
 *
 *        visualizer.addNode(5);
 *        visualizer.addNode(6);
 *        visualizer.addNode(1);
 *        visualizer.addNode(7);
 *        visualizer.resizeNode(1, 100, 100);
 *        visualizer.scaleNode(1, 0.3);
 *        visualizer.addConnection(5, 6);
 *        visualizer.setNodeColor(5, Color.RED);
 *        
 *        visualizer.visualize();
 *        
 *        visualizer.addConnection(1, 7);
 *        visualizer.setNodeColor(6, Color.GRAY);
 *        visualizer.addNode(9);
 *        visualizer.addNode(10);
 * </pre>
 */