package com.offbynull.peernetic.visualization;

import org.apache.commons.lang3.Validate;

public final class RemoveNode {
    private final String id;

    public RemoveNode(String id) {
        Validate.notNull(id);
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
