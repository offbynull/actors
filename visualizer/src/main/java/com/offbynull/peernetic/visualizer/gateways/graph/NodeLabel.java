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

import javafx.scene.control.Label;
import org.apache.commons.lang3.Validate;

final class NodeLabel extends Label {
    private static final String COLOR_STRING_FORMAT = "#%06X";
    
    private int color;
    private boolean temporary;

    public NodeLabel(String id) {
        super(id);
        
        Validate.notNull(id);
    
        setColor(0xFFFFFF); //white
    }

    public int getColor() {
        return color;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public void setColor(int color) {
        Validate.isTrue((color & 0xFF000000) == 0); // no alphachannel
        this.color = color;
        applyStyle();
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
        applyStyle();
    }
    
    private void applyStyle() {
        String style = "-fx-background-color: " + String.format(COLOR_STRING_FORMAT, color);
        if (temporary) {
            style += "; -fx-opacity: 0.5";
        }
        
        setStyle(style);
    }
}
