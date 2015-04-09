package com.offbynull.peernetic.visualization;

import org.apache.commons.lang3.Validate;

public final class MoveNode {
    private final String id;
    private final double x;
    private final double y;

    public MoveNode(String id, double x, double y) {
        Validate.notNull(id);
        Validate.isTrue(Double.isFinite(x));
        Validate.isTrue(Double.isFinite(y));
        this.id = id;
        this.x = x;
        this.y = y;
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
    
}
