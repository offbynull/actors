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

import javafx.scene.shape.Line;

final class EdgeLine extends Line {
    private static final String COLOR_STRING_FORMAT = "#%06X";
    
    private int color;
    private double width;

    public EdgeLine() {
        setColor(0x000000); // black
        setWidth(1.0);
    }
    

    public int getColor() {
        return color;
    }

    public double getWidth() {
        return width;
    }
    
    public void setColor(int color) {
        this.color = color;
        applyStyle();
    }

    public void setWidth(double width) {
        this.width = width;
        applyStyle();
    }
    
    private void applyStyle() {
        String style = "-fx-stroke-width: " + width + "; -fx-stroke: " + String.format(COLOR_STRING_FORMAT, color);
        setStyle(style);
    }

    
}
