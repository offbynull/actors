package com.offbynull.peernetic.overlay.chord.tasks;

import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.helpers.AbstractChainedTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import org.apache.commons.lang3.Validate;

public final class StabilizeTask<A> extends AbstractChainedTask {
    private ChordState<A> chordState;
    
    private EndpointFinder<A> finder;

    private Stage stage = Stage.INITIAL;

    public StabilizeTask(ChordState<A> chordState) {
        Validate.notNull(chordState);
        Validate.notNull(finder);
    }

    @Override
    protected Task switchTask(Task prev) {
        if (prev != null && prev.getState() == TaskState.FAILED) {
            setFinished(true);
            return null;
        }
        
        switch (stage) {
            case INITIAL: {
                Pointer<A> successor = chordState.getSuccessor();
                return new NotifyTask(chordState.getBase(), successor, finder);
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
