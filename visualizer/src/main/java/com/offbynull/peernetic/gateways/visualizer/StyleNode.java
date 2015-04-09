package com.offbynull.peernetic.gateways.visualizer;

import org.apache.commons.lang3.Validate;

public final class StyleNode {
    private final String id;
    private final String style;

    public StyleNode(String id, String style) {
        Validate.notNull(id);
        Validate.notNull(style);
        this.id = id;
        this.style = style;
    }

    public String getId() {
        return id;
    }

    public String getStyle() {
        return style;
    }
    
}
