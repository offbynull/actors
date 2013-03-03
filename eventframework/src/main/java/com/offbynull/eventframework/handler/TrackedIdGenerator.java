package com.offbynull.eventframework.handler;

public final class TrackedIdGenerator {
    private long id;
    
    public long getNextId() {
        return id++;
    }
}
