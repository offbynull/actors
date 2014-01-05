package com.offbynull.peernetic.actor.helpers;

public final class NotifyManager {
    private long hitTimestamp = Long.MAX_VALUE;
    
    public boolean process(long timestamp) {
        return timestamp >= hitTimestamp;
    }
    
    public void reset(long nextTimestamp) {
        hitTimestamp = nextTimestamp;
    }

    public long getNextTimeoutTimestamp() {
        return hitTimestamp;
    }
}
