package com.offbynull.peernetic.gateways.visualizer;

import org.apache.commons.lang3.Validate;

public final class AddNode {
    private final String id;
    private final double x;
    private final double y;
    private final String style;

    public AddNode(String id) {
        this(id, 0.0, 0.0);
    }

    public AddNode(String id, double x, double y) {
        this(id, x, y, "");
    }

    public AddNode(String id, double x, double y, String style) {
        Validate.notNull(id);
        Validate.isTrue(Double.isFinite(x));
        Validate.isTrue(Double.isFinite(y));
        Validate.notNull(style);
        this.id = id;
        this.x = x;
        this.y = y;
        this.style = style;
    }

    public String getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getStyle() {
        return style;
    }
    
}
