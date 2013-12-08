package com.offbynull.overlay.visualizer;

public interface NodePlacer<A> {
    NodePlacementInfo placeNode(A node);
}
