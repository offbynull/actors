package com.offbynull.peernetic.overlay.unstructured;

import java.util.HashSet;
import java.util.Set;

public final class State<A> {
    private Set<A> incomingLinks;
    private Set<A> outgoingLinks;
    private boolean incomingLinksFull;

    public State(Set<A> incomingLinks, Set<A> outgoingLinks, boolean incomingLinksFull) {
        this.incomingLinks = new HashSet<>(incomingLinks);
        this.outgoingLinks = new HashSet<>(incomingLinks);
        this.incomingLinksFull = incomingLinksFull;
    }

    public Set<A> getIncomingLinks() {
        return incomingLinks;
    }

    public Set<A> getOutgoingLinks() {
        return outgoingLinks;
    }

    public boolean isIncomingLinksFull() {
        return incomingLinksFull;
    }
    
}
