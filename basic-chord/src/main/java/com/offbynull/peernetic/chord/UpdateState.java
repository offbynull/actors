package com.offbynull.peernetic.chord;

import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Provides instructions on how to update fingers and stabilize the successor.
 * @author Kasra Faghihi
 */
public final class UpdateState {
    /**
     * Milliseconds to wait before stabilizing.
     */
    private static final long STABILIZE_DURATION = 5000L;
    
    /**
     * Milliseconds to wait before fixing a finger.
     */
    private static final long FIX_FINGER_DURATION = 5000L;
    
    /**
     * Simultaneous finger update count.
     */
    private static final int FINGER_COUNT = 3;
    
    /**
     * Base pointer.
     */
    private BitLimitedPointer basePtr;
    
    /**
     * Current finger index.
     */
    private int index;

    /**
     * Construct a {@link UpdateState} object.
     * @param basePtr base pointer
     * @throws NullPointerException if any arguments are {@code null}
     */
    public UpdateState(BitLimitedPointer basePtr) {
        if (basePtr == null) {
            throw new NullPointerException();
        }
        this.basePtr = basePtr;
        this.index = 1;
    }
    
    /**
     * Get the next finger update instruction.
     * @return next finger update instruction
     */
    public FingerUpdateInstruction getNextFingerUpdate() {
        Set<Integer> fingers = new LinkedHashSet<>();
        
        for (int i = 0; i < FINGER_COUNT; i++) {
            fingers.add(index);
            index++;
            if (index > basePtr.getId().getBitCount()) {
                index = 1;
            }
        }
        
        return new FingerUpdateInstruction(fingers, FIX_FINGER_DURATION);
    }

    /**
     * Get the next successor stabilize instruction.
     * @return next stabilize update instruction
     */
    public SuccessorStabilizeInstruction getNextStabilizeUpdate() {        
        return new SuccessorStabilizeInstruction(STABILIZE_DURATION);
    }
}
