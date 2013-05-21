package com.offbynull.peernetic.chord;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The fingers to update and the amount of time to wait before getting the next
 * set of fingers to update.
 * @author Kasra Faghihi
 */
public final class FingerUpdateInstruction {
    /**
     * Fingers to update.
     */
    private Set<Integer> fingers;
    /**
     * Milliseconds to wait.
     */
    private long waitDuration;

    /**
     * Constructs a {@link FingerUpdateInstruction} object.
     * @param fingers fingers to update
     * @param waitDuration milliseconds to wait
     * @throws NullPointerException if any argument is {@code null}
     * @throws NullPointerException if any collection argument contains
     * {@code null} 
     * @throws IllegalArgumentException if any numeric argument is {@code < 0}
     */
    public FingerUpdateInstruction(Collection<Integer> fingers, long waitDuration) {
        if (fingers == null || fingers.contains(null)) {
            throw new NullPointerException();
        }
        
        if (waitDuration < 0L) {
            throw new IllegalArgumentException();
        }
        
        this.fingers = Collections.unmodifiableSet(new LinkedHashSet<>(fingers));
        this.waitDuration = waitDuration;
    }

    /**
     * View the finger indexes to be updated.
     * @return finger indexes to be updated
     */
    public Set<Integer> viewFingers() {
        return fingers;
    }

    /**
     * Get the number of milliseconds to wait before updating a new set of
     * fingers.
     * @return milliseconds to wait before next update
     */
    public long getWaitDuration() {
        return waitDuration;
    }
}
