package com.offbynull.peernetic.overlay.unstructured;

public interface OverlayListener<A> {
    void linkEstablished(A address, LinkType type);
    void linkBroken(A address, LinkType type);
    
    public enum LinkType {
        INCOMING,
        OUTGOING
    }
}
