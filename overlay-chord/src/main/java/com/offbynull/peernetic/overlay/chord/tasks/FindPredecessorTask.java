package com.offbynull.peernetic.overlay.chord.tasks;

import com.offbynull.peernetic.actor.helpers.AbstractChainedTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.offbynull.peernetic.overlay.chord.core.RouteResult;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import org.apache.commons.lang3.Validate;

final class FindPredecessorTask<A> extends AbstractChainedTask {
    private Stage stage = Stage.INITIAL;
    
    private Id findId;
    private ChordState<A> chordState;
    
    private EndpointFinder<A> finder;
    
    private Pointer<A> lastClosestPredecessor;
    
    private Pointer<A> result;

    public FindPredecessorTask(Id findId, ChordState<A> chordState, EndpointFinder<A> finder) {
        Validate.notNull(findId);
        Validate.notNull(chordState);
        Validate.notNull(finder);

        this.findId = findId;
        this.chordState = chordState;
        this.finder = finder;
    }
    
    public Pointer<A> getResult() {
        return result;
    }

    @Override
    protected Task switchTask(Task prev) {
        if (prev != null && prev.getState() == TaskState.FAILED) {
            setFinished(true);
            return null;
        }
        
        
        switch (stage) {
            case INITIAL: {
                result = chordState.getBase();
                RouteResult<A> routeResult = chordState.route(findId);

                if (routeResult.getResultType() == RouteResult.ResultType.SELF) {
                    result = routeResult.getPointer();
                    setFinished(false);
                    return null;
                }

                stage = Stage.FINDING_LAST_CLOSEST_PREDECESSOR;
                return new GetClosestPrecedingFingerTask(findId, routeResult.getPointer(), finder);
            }
            case FINDING_LAST_CLOSEST_PREDECESSOR: {
                lastClosestPredecessor = ((GetClosestPrecedingFingerTask) prev).getResult();
                stage = Stage.FINDING_LAST_CLOSEST_PREDECESSORS_SUCCESSOR;
                return new GetSuccessorTask(lastClosestPredecessor, finder);
            }
            case FINDING_LAST_CLOSEST_PREDECESSORS_SUCCESSOR: {
                Pointer<A> successor = ((GetSuccessorTask) prev).getResult();

                Id lastClosestPredId = lastClosestPredecessor.getId();
                Id successorId = successor.getId();

                if (findId.isWithin(lastClosestPredId, false, successorId, true)) {
                    setFinished(false);
                    result = lastClosestPredecessor;
                    return null;
                }

                stage = Stage.FINDING_LAST_CLOSEST_PREDECESSOR;
                return new GetClosestPrecedingFingerTask(findId, lastClosestPredecessor, finder);
            }
            default:
                throw new IllegalStateException();
        }
    }
    
    private enum Stage {
        INITIAL,
        FINDING_LAST_CLOSEST_PREDECESSOR,
        FINDING_LAST_CLOSEST_PREDECESSORS_SUCCESSOR
    }
}
