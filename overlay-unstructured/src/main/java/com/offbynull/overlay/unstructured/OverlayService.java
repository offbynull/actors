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

        private Set<A> neighbours;
        private int maxNeighbours;

        public Information(Set<A> neighbours, int maxNeighbours) {
            super();
            this.neighbours = Collections.unmodifiableSet(new HashSet<>(neighbours));
            this.maxNeighbours = maxNeighbours;
        }

        public Set<A> getNeighbours() {
            return neighbours;
        }

        public int getMaxNeighbours() {
            return maxNeighbours;
        }
    }
}
