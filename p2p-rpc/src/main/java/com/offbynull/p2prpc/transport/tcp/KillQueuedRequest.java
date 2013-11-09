package com.offbynull.p2prpc.transport.tcp;

final class KillQueuedRequest implements OutgoingRequest {
    private long id;

    KillQueuedRequest(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
    
}
