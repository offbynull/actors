package com.offbynull.peernetic.common.concurrent.actor;

final class IdCounter {
    private long id;
    
    public long getNext() {
        return id++;
    }
    
}
