package com.offbynull.peernetic.examples.raft;

import java.io.Serializable;

public final class LogEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final int term;
    private final Object value; // should actually be a command like SET/ADD/etc.., but we just set to value for simplicity

    public LogEntry(int term, Object value) {
        this.term = term;
        this.value = value; // value may be null
    } // value may be null

    public int getTerm() {
        return term;
    }

    public Object getValue() {
        return value;
    }
    
}
