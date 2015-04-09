package com.offbynull.peernetic.visualization;

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
