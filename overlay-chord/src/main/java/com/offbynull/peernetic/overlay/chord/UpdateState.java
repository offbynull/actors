package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.overlay.common.id.BitLimitedPointer;

/**
 * Provides instructions on how to update fingers and stabilize the successor.
 *
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
     * Base pointer.
     */
    private BitLimitedPointer basePtr;
    /**
     * Current finger index.
     */
    private int index;

    /**
     * Construct a {@link UpdateState} object.
     *
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
     *
     * @return next finger update instruction
     */
    public FingerUpdateInstruction getNextFingerUpdate() {
        int fingerIdx = index;
        index++;
        if (index > basePtr.getId().getBitCount()) {
            index = 1;
        }

        return new FingerUpdateInstruction(fingerIdx, FIX_FINGER_DURATION);
    }

    /**
     * Get the next successor stabilize instruction.
     *
     * @return next stabilize update instruction
     */
    public SuccessorStabilizeInstruction getNextStabilizeUpdate() {
        return new SuccessorStabilizeInstruction(STABILIZE_DURATION);
    }

    /**
     * The amount of time to wait before stabilizing the successor.
     *
     * @author Kasra Faghihi
     */
    public final static class SuccessorStabilizeInstruction {

        /**
         * Milliseconds to wait.
         */
        private long waitDuration;

        /**
         * Constructs a {@link SuccessorStabilizeInstruction} object.
         *
         * @param waitDuration milliseconds to wait
         * @throws IllegalArgumentException if any numeric argument is          {@code < 0}
         */
        public SuccessorStabilizeInstruction(long waitDuration) {
            if (waitDuration < 0L) {
                throw new IllegalArgumentException();
            }

            this.waitDuration = waitDuration;
        }

        /**
         * Get the number of milliseconds to wait before stabilizing.
         *
         * @return milliseconds to wait before stabilizing
         */
        public long getWaitDuration() {
            return waitDuration;
        }
    }

    /**
     * The finger to update and the amount of time to wait before getting the
     * next finger to update.
     *
     * @author Kasra Faghihi
     */
    public static final class FingerUpdateInstruction {

        /**
         * Finger index to update.
         */
        private int finger;
        /**
         * Milliseconds to wait.
         */
        private long waitDuration;

        /**
         * Constructs a {@link FingerUpdateInstruction} object.
         *
         * @param finger finger index to update
         * @param waitDuration milliseconds to wait
         * @throws IllegalArgumentException if {@code waitDuration < 0}
         * @
         * throws IllegalArgumentException if {@code finger < 1}
         */
        public FingerUpdateInstruction(int finger,
                long waitDuration) {
            if (finger < 1 || waitDuration < 0L) {
                throw new IllegalArgumentException();
            }

            this.finger = finger;
            this.waitDuration = waitDuration;
        }

        /**
         * Get the finger index to be updated.
         *
         * @return finger index to be updated
         */
        public int getFinger() {
            return finger;
        }

        /**
         * Get the number of milliseconds to wait before updating a new set of
         * fingers.
         *
         * @return milliseconds to wait before next update
         */
        public long getWaitDuration() {
            return waitDuration;
        }
    }
}
