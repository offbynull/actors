/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.visualizer.gateways.graph;

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
