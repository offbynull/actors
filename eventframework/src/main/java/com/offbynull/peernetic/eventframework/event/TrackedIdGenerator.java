package com.offbynull.peernetic.eventframework.event;

public final class TrackedIdGenerator {
    private long id;
    
    public long getNextId() {
        return id++;
    }
}
