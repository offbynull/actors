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
/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.gateways.visualizer;

import java.awt.Point;
import org.apache.commons.lang3.Validate;

/**
 * Utility methods for generating positions on a 2D plane.
 * @author Kasra Faghihi
 */
public final class PositionUtils {
    private PositionUtils() {
        // do nothing
    }
    
    /**
     * Creates a random point within a rectangle.
     * @param width rectangle width
     * @param height rectangle height
     * @return new random point within rectangle specified by {@code width} and {@code height}
     * @throws IllegalArgumentException if any argument is negative or a special double value (e.g. NaN/infinite/etc..)
     */
    public static Point randomPointInRectangle(double width, double height) {
        Validate.inclusiveBetween(0.0, Double.MAX_VALUE, width);
        Validate.inclusiveBetween(0.0, Double.MAX_VALUE, height);
        return new Point((int) (Math.random() * width), (int) (Math.random() * height));
    }

    /**
     * Creates point on a circle.
     * @param radius radius of circle
     * @param percentage 0 to 1 percentage of where point is on circle -- 0.0 indicates that the point is at the top middle
     * @return new point on circle specified by {@code radius} and {@code percentage}
     * @throws IllegalArgumentException if {@code radius} is negative or a special double value (e.g. NaN/infinite/etc..), or if
     * {@code percentage} isn't between {@code 0.0 - 1.0}
     */
    public static Point pointOnCircle(double radius, double percentage) {
        Validate.inclusiveBetween(0.0, Double.MAX_VALUE, radius);
        Validate.inclusiveBetween(0.0, 1.0, percentage);
        double angle = percentage * Math.PI * 2.0;
        angle -= Math.PI / 2.0; // adjust so that percentage 0.0 is at top middle, if not it'ld be at middle right
        
        double y = (Math.sin(angle) * radius) + radius; // NOPMD
        double x = (Math.cos(angle) * radius) + radius; // NOPMD
        
        return new Point((int) x, (int) y);
    }
}