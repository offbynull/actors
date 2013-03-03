package com.offbynull.eventframework.helper;

/**
 * Tracks a started/stopped state. This class is intended to be used as a part
 * of another service-style class that can be started and stopped. Once stopped,
 * it won't allow starting again.
 * 
 * @author Kasra Faghihi
 */
public final class StateTracker {

    private State state;

    public StateTracker() {
        state = State.NONE;
    }

    public void start() {
        if (state != State.NONE) {
            throw new IllegalStateException();
        }

        state = State.STARTED;
    }

    public void stop() {
        if (state != State.STARTED) {
            throw new IllegalStateException();
        }
        
         state = State.STOPPED;
    }

    public void checkStarted() {
        if (state != State.STARTED) {
            throw new IllegalStateException();
        }
    }

    public void checkStopped() {
        if (state != State.STOPPED) {
            throw new IllegalStateException();
        }
    }

    public void checkFresh() {
        if (state != State.NONE) {
            throw new IllegalStateException();
        }
    }

    public boolean isStarted() {
        return state == State.STARTED;
    }

    public boolean isStopped() {
        return state == State.STARTED;
    }

    public boolean isFresh() {
        return state == State.NONE;
    }

    private enum State {

        NONE,
        STARTED,
        STOPPED,
    }
}
