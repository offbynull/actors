package com.offbynull.overlay.unstructured;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public interface OverlayService<A> {
    public static final int SERVICE_ID = 5000;
    public static final int SECRET_SIZE = 16;
    
    Information<A> getInformation();
    boolean join(byte[] secret);
    void unjoin(byte[] secret);
    boolean keepAlive(byte[] secret);

    public static final class Information<A> {

        private Set<A> incomingLinks;
        private Set<A> outgoingLinks;
        private boolean incomingLinksFull;

        public Information(Set<A> incomingLinks, Set<A> outgoingLinks, boolean incomingLinksFull) {
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
}
