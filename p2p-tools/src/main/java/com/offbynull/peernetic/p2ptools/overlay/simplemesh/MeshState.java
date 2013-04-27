package com.offbynull.peernetic.p2ptools.overlay.simplemesh;

import com.offbynull.peernetic.p2ptools.identification.LimitedPointer;

public final class MeshState {
    private PeerTable peerTable;

    public MeshState(int maxPeerCount) {
        peerTable = new PeerTable(maxPeerCount);
    }

    public boolean containsPeer(LimitedPointer pointer) {
        return peerTable.contains(pointer);
    }

    public boolean addPeer(LimitedPointer pointer) {
        return peerTable.add(pointer);
    }

    public boolean removePeer(LimitedPointer pointer) {
        return peerTable.remove(pointer);
    }
    
    public int getPeerCount() {
        return peerTable.getPeerCount();
    }

    public int getMaxPeerCount() {
        return peerTable.getMaxPeerCount();
    }

    public int getPeerSlotsAvailable() {
        return peerTable.getPeerSlotsAvailable();
    }    
}
