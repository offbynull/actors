package com.offbynull.peernetic.chord;

/**
 * The amount of time to wait before stabilizing the successor.
 * @author Kasra Faghihi
 */
public final class SuccessorStabilizeInstruction {
    /**
     * Milliseconds to wait.
     */
    private long waitDuration;

    /**
     * Constructs a {@link SuccessorStabilizeInstruction} object.
     * @param waitDuration milliseconds to wait
     * @throws IllegalArgumentException if any numeric argument is {@code < 0}
     */
    public SuccessorStabilizeInstruction(long waitDuration) {
        if (waitDuration < 0L) {
            throw new IllegalArgumentException();
        }
                
        this.waitDuration = waitDuration;
    }

    /**
     * Get the number of milliseconds to wait before stabilizing.
     * @return milliseconds to wait before stabilizing
     */
    public long getWaitDuration() {
        return waitDuration;
    }
}
