package com.offbynull.peernetic.p2ptools.overlay.simplemesh;

import com.offbynull.peernetic.p2ptools.identification.LimitedPointer;
import java.util.HashSet;
import java.util.Set;

public final class PeerTable {
    private Set<LimitedPointer> addresses = new HashSet<>();
    private int maxPeerCount;

    public PeerTable(int maxPeerCount) {
        if (maxPeerCount < 0) {
            throw new IllegalArgumentException();
        }
        
        this.maxPeerCount = maxPeerCount;
    }

    public boolean contains(LimitedPointer pointer) {
        if (pointer == null) {
            throw new NullPointerException();
        }
        return addresses.contains(pointer);
    }

    public boolean add(LimitedPointer pointer) {
        if (pointer == null) {
            throw new NullPointerException();
        }
        
        if (addresses.size() == maxPeerCount) {
            return false;
        }
        
        return addresses.add(pointer);
    }

    public boolean remove(LimitedPointer pointer) {
        if (pointer == null) {
            throw new NullPointerException();
        }
        return addresses.remove(pointer);
    }

    public void clear() {
        addresses.clear();
    }
    
    public int getPeerCount() {
        return addresses.size();
    }
    
    public int getMaxPeerCount() {
        return maxPeerCount;
    }
    
    public int getPeerSlotsAvailable() {
        return maxPeerCount - addresses.size();
    }
}
