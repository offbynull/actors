package com.offbynull.overlay.visualizer;

import org.apache.commons.lang3.Validate;

public final class AddEdgeCommand<A> implements Command<A> {
    private A from;
    private A to;

    public AddEdgeCommand(A from, A to) {
        Validate.notNull(from);
        Validate.notNull(to);
        
        this.from = from;
        this.to = to;
    }

    public A getFrom() {
        return from;
    }

    public A getTo() {
        return to;
    }
    
}
