package com.offbynull.overlay.visualizer;

import java.awt.Color;
import java.util.Random;
import org.apache.commons.lang3.Validate;

public final class RandomLocationNodePlacer<A> implements NodePlacer<A> {
    
    public Random random;
    private int width;
    private int height;

    public RandomLocationNodePlacer(int width, int height) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, width);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, height);
        random = new Random();
        this.width = width;
        this.height = height;
    }
    

    @Override
    public NodePlacementInfo placeNode(A node) {
        return new NodePlacementInfo(random.nextInt(width), random.nextInt(height), 1.0, Color.CYAN);
    }
    
}
