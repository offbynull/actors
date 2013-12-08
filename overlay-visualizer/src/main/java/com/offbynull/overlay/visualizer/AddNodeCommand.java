package com.offbynull.overlay.visualizer;

import org.apache.commons.lang3.Validate;

public final class AddNodeCommand<A> implements Command<A> {
    private A node;

    public AddNodeCommand(A node) {
        Validate.notNull(node);
        
        this.node = node;
    }

    public A getNode() {
        return node;
    }
}
