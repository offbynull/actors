package com.offbynull.overlay.visualizer;

import java.awt.Color;
import org.apache.commons.lang3.Validate;

public final class NodePlacementInfo {
    private int centerX;
    private int centerY;
    private double scale;
    private Color color;

    public NodePlacementInfo(int centerX, int centerY, double scale, Color color) {
        Validate.inclusiveBetween(0.0, Double.POSITIVE_INFINITY, scale);
        Validate.notNull(color);
        Validate.isTrue(color.getAlpha() == 255);
        
        this.centerX = centerX;
        this.centerY = centerY;
        this.scale = scale;
        this.color = color;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public double getScale() {
        return scale;
    }

    public Color getColor() {
        return color;
    }

}
