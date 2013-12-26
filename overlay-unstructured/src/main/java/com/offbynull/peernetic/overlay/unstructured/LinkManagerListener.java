package com.offbynull.peernetic.overlay.unstructured;

public interface LinkManagerListener<A> {
    void linkCreated(LinkType type, A address);
    void linkDestroyed(LinkType type, A address);
}
