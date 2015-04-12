package com.offbynull.peernetic.examples.unstructured.externalmessages;

public abstract class ExternalMessage {
    private long id;

    public ExternalMessage(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
    
}
