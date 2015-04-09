package com.offbynull.peernetic.gateways.visualizer;

import org.apache.commons.lang3.Validate;

public final class AddEdge {
    private final String fromId;
    private final String toId;

    public AddEdge(String fromId, String toId) {
        Validate.notNull(fromId);
        Validate.notNull(toId);
        this.fromId = fromId;
        this.toId = toId;
    }

    public String getFromId() {
        return fromId;
    }

    public String getToId() {
        return toId;
    }
    
}
