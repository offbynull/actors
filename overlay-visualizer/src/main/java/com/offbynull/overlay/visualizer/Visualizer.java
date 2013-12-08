package com.offbynull.overlay.visualizer;

import java.awt.Color;

public interface Visualizer<A> {

    void addConnection(final A from, final A to);

    void addNode(final A address);

    void moveNode(final A address, final int centerX, final int centerY);

    void removeConnection(final A from, final A to);

    void removeNode(final A address);

    void resizeNode(final A address, final int width, final int height);

    void scaleNode(final A address, final double scale);

    void setNodeColor(final A address, final Color color);

    void visualize();
    
}
