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
package com.offbynull.peernetic.gateways.visualizer;

import org.apache.commons.lang3.Validate;

public final class StyleEdge {
    private final String fromId;
    private final String toId;
    private final String style;

    public StyleEdge(String fromId, String toId, String style) {
        Validate.notNull(fromId);
        Validate.notNull(toId);
        Validate.notNull(style);
        
        this.fromId = fromId;
        this.toId = toId;
        this.style = style;
    }

    public String getFromId() {
        return fromId;
    }

    public String getToId() {
        return toId;
    }

    public String getStyle() {
        return style;
    }
    
}
