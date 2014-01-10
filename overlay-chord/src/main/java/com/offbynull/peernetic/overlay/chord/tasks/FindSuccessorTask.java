package com.offbynull.peernetic.overlay.chord.tasks;

import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.helpers.RequestManager;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import org.apache.commons.lang3.Validate;

public final class FindSuccessorTask<A> {
    private Id findId;
    private ChordState<A> chordState;
    private TaskState taskState;
    
    private RequestManager requestManager;
    private EndpointFinder<A> finder;
    
    private FindPredecessorTask<A> findPredecessorTask;
    private GetSuccessorTask<A> getSuccessorTask;
    
    private Pointer<A> result;

    public FindSuccessorTask(Id findId, ChordState<A> chordState, EndpointFinder<A> finder) {
        Validate.notNull(findId);
        Validate.notNull(chordState);
        Validate.notNull(finder);

        this.findId = findId;
        this.chordState = chordState;
        this.finder = finder;
        
        taskState = TaskState.START;
    }
    
    public long process(long timestamp, Incoming incoming, PushQueue pushQueue) {
        switch (taskState) {
            case START: {
                start(timestamp, incoming, pushQueue);
                break;
            }
            case FIND_PREDECESSOR: {
                queryFindPredecessorTask(timestamp, incoming, pushQueue);
                break;
            }
            case FIND_SUCCESSOR: {
                queryGetSuccessorHandler();
                break;
            }
            case FAILED:
            case SUCCESSFUL: {
                break;
            }
            default:
                throw new IllegalStateException();
        }
        
        return requestManager.process(timestamp, pushQueue);
    }
    
    private void start(long timestamp, Incoming incoming, PushQueue pushQueue) {
        taskState = TaskState.FIND_PREDECESSOR;
        
        findPredecessorTask = new FindPredecessorTask<>(findId, chordState, finder);
        findPredecessorTask.process(timestamp, incoming, pushQueue); // need an initial call to process to make sure this starts
    }
    
    private void queryFindPredecessorTask(long timestamp, Incoming incoming, PushQueue pushQueue) {
        findPredecessorTask.process(timestamp, incoming, pushQueue);
        
        switch (findPredecessorTask.getState()) {
            case START:
            case NEXT_CLOSEST_PREDECESSOR_PERFORMING:
            case CLOSEST_PREDECESSOR_SUCCESSOR_PERFORMING:
                break;
            case SUCCESSFUL:
                Pointer<A> closestPredecessor = findPredecessorTask.getResult();
                
                getSuccessorTask = new GetSuccessorTask(closestPredecessor, finder);
                getSuccessorTask.process(timestamp, incoming, pushQueue); // need an initial call to process to make sure this starts
                
                taskState = TaskState.FIND_SUCCESSOR;
                break;
            case FAILED:
                taskState = TaskState.FAILED;
                break;
        }
    }
    
    private void queryGetSuccessorHandler() {
        switch (getSuccessorTask.getState()) {
            case PROCESSING:
                break;
            case COMPLETED:
                Pointer<A> successor = getSuccessorTask.getResult();
                
                taskState = TaskState.SUCCESSFUL;
                result = successor;
                break;
            case FAILED:
                taskState = TaskState.FAILED;
                break;
            default:
                throw new IllegalStateException();
        }
    }
    
    public TaskState getState() {
        return taskState;
    }
    
    public Pointer<A> getResult() {
        return result;
    }

    public enum TaskState {
        START,
        FIND_PREDECESSOR,
        FIND_SUCCESSOR,
        SUCCESSFUL,
        FAILED
    }

}
