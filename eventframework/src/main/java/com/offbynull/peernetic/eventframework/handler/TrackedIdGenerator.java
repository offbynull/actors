package com.offbynull.peernetic.eventframework.handler;

public final class TrackedIdGenerator {
    private long id;
    
    public long getNextId() {
        return id++;
    }
}
