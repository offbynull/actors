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
        
        
        if (prev == null) {
            result = chordState.getBase();
            RouteResult<A> routeResult = chordState.route(findId);

            if (routeResult.getResultType() == RouteResult.ResultType.SELF) {
                result = routeResult.getPointer();
                setFinished(false);
                return null;
            }
            
            return new GetClosestPrecedingFingerTask(findId, routeResult.getPointer(), finder);
        } else if (prev instanceof GetClosestPrecedingFingerTask) {
            lastClosestPredecessor = ((GetClosestPrecedingFingerTask) prev).getResult();
            return new GetSuccessorTask(lastClosestPredecessor, finder);
        } else if (prev instanceof GetSuccessorTask) {
            Pointer<A> successor = ((GetSuccessorTask) prev).getResult();

            Id lastClosestPredId = lastClosestPredecessor.getId();
            Id successorId = successor.getId();

            if (findId.isWithin(lastClosestPredId, false, successorId, true)) {
                setFinished(false);
                result = lastClosestPredecessor;
                return null;
            }

            return new GetClosestPrecedingFingerTask(findId, lastClosestPredecessor, finder);
        }
        
        throw new IllegalStateException();
    }
}
