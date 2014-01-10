package com.offbynull.peernetic.overlay.chord.tasks;

import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.helpers.AbstractChainedTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import org.apache.commons.lang3.Validate;

public final class FixFingerTask<A> extends AbstractChainedTask {
    
    private ChordState<A> chordState;
    private int idx;
    
    private EndpointFinder<A> finder;

    private Stage stage = Stage.INITIAL;

    public FixFingerTask(ChordState<A> chordState, EndpointFinder<A> finder, int idx) {
        Validate.notNull(chordState);
        Validate.notNull(finder);
        Validate.inclusiveBetween(1, chordState.getBitCount(), idx); // cannot be 0
        
        this.chordState = chordState;
        this.finder = finder;
        this.idx = idx;
    }
    
    

    @Override
    protected Task switchTask(Task prev) {
        if (prev != null && prev.getState() == TaskState.FAILED) {
            setFinished(true);
            return null;
        }
        
        switch (stage) {
            case INITIAL: {
                return new FindSuccessorTask(chordState.getExpectedFingerId(idx), chordState, finder);
            }
            case FIND_SUCCESSOR: {
                Pointer<A> result = ((FindSuccessorTask) prev).getResult();
                chordState.putFinger(result);
                
                setFinished(false);
                return null;
            }
            default:
                throw new IllegalStateException();
        }
    }
    
    private enum Stage {
        INITIAL,
        FIND_SUCCESSOR
    }
}
