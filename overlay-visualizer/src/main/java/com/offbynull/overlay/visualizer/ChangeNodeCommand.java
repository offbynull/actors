package com.offbynull.overlay.visualizer;

import java.awt.Color;
import java.awt.Point;
import org.apache.commons.lang3.Validate;

public final class ChangeNodeCommand<A> implements Command<A> {
    private A node;
    private Double scale;
    private Point location;
    private Color color;

    public ChangeNodeCommand(A node, Double scale, Point location, Color color) {
        Validate.notNull(node); // others can be null
        Validate.isTrue(scale == null || scale >= 0.0, "Negative scale");
        
        this.node = node;
        this.scale = scale;
        this.location = location;
        this.color = color;
    }

    public A getNode() {
        return node;
    }

    public Double getScale() {
        return scale;
    }

    public Point getCenter() {
        return location;
    }

    public Color getColor() {
        return color;
    }
    
}
