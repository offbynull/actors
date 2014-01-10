package com.offbynull.peernetic.overlay.chord.tasks;

import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.IdGenerator;
import com.offbynull.peernetic.overlay.common.id.IdUtils;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.Random;
import org.apache.commons.lang3.Validate;

public class JoinTask<A> {
    private int nextFingerIdx;
    private Pointer<A> bootstrap;
    private TaskState taskState;
    
    private EndpointFinder<A> finder;
    
    private FindSuccessorTask<A> findSuccessorTask;

    private ChordState<A> chordState;

    public JoinTask(A selfAddress, Random random, Pointer<A> bootstrap, EndpointFinder<A> finder) {
        Validate.notNull(selfAddress);
        Validate.notNull(random);
        Validate.notNull(bootstrap);
        Validate.notNull(finder);

        Id bootstrapId = bootstrap.getId();
        IdUtils.ensureLimitPowerOfTwo(bootstrapId);
        
        IdGenerator idGenerator = new IdGenerator(random);
        Id selfId = idGenerator.generate(bootstrapId.getLimitAsByteArray());
        
        Pointer<A> basePtr = new Pointer(selfId, selfAddress);
        
        this.bootstrap = bootstrap;
        this.chordState = new ChordState<>(basePtr);
        this.finder = finder;
        
        taskState = TaskState.START;
    }
    
    public long process(long timestamp, Incoming incoming, PushQueue pushQueue) {
        switch (taskState) {
            case START: {
                start(timestamp, incoming, pushQueue);
                break;
            }
            case POPULATE_FINGERS: {
                queryFindSuccessorTask(timestamp, incoming, pushQueue);
                break;
            }
            case FAILED:
            case SUCCESSFUL: {
                break;
            }
            default:
                throw new IllegalStateException();
        }
        
        return Long.MAX_VALUE;
    }

    private void start(long timestamp, Incoming incoming, PushQueue pushQueue) {
        taskState = TaskState.POPULATE_FINGERS;
        
        chordState.putFinger(bootstrap);
        Id selfId = chordState.getBaseId(); // pass in id we set for ourself, because we want our successor
        
        // find successor to self
        findSuccessorTask = new FindSuccessorTask<>(selfId, chordState, finder);
        findSuccessorTask.process(timestamp, incoming, pushQueue); // need an initial call to process to make sure this starts
    }

    private void queryFindSuccessorTask(long timestamp, Incoming incoming, PushQueue pushQueue) {
        findSuccessorTask.process(timestamp, incoming, pushQueue);
        
        switch (findSuccessorTask.getState()) {
            case START:
            case FIND_PREDECESSOR:
            case FIND_SUCCESSOR:
                break;
            case SUCCESSFUL:
                Pointer<A> successor = findSuccessorTask.getResult();
                chordState.putFinger(successor);
                
                nextFingerIdx++;
                if (chordState.getBitCount() == nextFingerIdx) {
                    NEEDS_TO_MOVE_TO_ASSUME_PREDECESSOR;
                    break;
                }
                
                Id nextId = chordState.getExpectedFingerId(nextFingerIdx).decrement(); // decrement because we want the successor to this id
                
                findSuccessorTask = new FindSuccessorTask<>(nextId, chordState, finder);
                findSuccessorTask.process(timestamp, incoming, pushQueue);
                break;
            case FAILED:
                taskState = TaskState.FAILED;
                break;
        }
    }

    public TaskState getState() {
        return taskState;
    }
    
    public ChordState<A> getResult() {
        return taskState == TaskState.SUCCESSFUL ? chordState : null;
    }

    public enum TaskState {
        START,
        POPULATE_FINGERS,
        ASSUME_PREDECESSOR, // grab the predecessor of your successor and set it as your predecessor
        NOTIFY_SUCCESSOR, // tell your successor that you're it's new predecessor
        SUCCESSFUL,
        FAILED
    }
}
