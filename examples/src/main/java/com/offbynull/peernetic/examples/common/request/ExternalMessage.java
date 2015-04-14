package com.offbynull.peernetic.examples.common.request;

public abstract class ExternalMessage {
    private long id;

    public ExternalMessage(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
    
}
