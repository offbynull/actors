package com.offbynull.overlay.visualizer;

public interface Visualizer<A> {

    void step(String output, Command<A> ... commands);
    void visualize();
    void visualize(VisualizerEventListener listener);
    
}
