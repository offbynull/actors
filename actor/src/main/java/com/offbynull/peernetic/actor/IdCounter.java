package com.offbynull.peernetic.actor;

final class IdCounter {
    private long id;
    
    public long getNext() {
        return id++;
    }
    
}
