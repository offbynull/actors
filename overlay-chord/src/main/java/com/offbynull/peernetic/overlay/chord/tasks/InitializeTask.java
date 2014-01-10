package com.offbynull.peernetic.overlay.chord.tasks;

import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.helpers.AbstractChainedTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.IdGenerator;
import com.offbynull.peernetic.overlay.common.id.IdUtils;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.Random;
import org.apache.commons.lang3.Validate;

public final class InitializeTask<A> extends AbstractChainedTask {
    private int nextFingerIdx;
    private Pointer<A> bootstrap;
    private Stage stage;
    
    private EndpointFinder<A> finder;

    private ChordState<A> chordState;

    public InitializeTask(A selfAddress, Random random, Pointer<A> bootstrap, EndpointFinder<A> finder) {
        Validate.notNull(selfAddress);
        Validate.notNull(random);
        Validate.notNull(finder);

        Id bootstrapId = bootstrap.getId();
        IdUtils.ensureLimitPowerOfTwo(bootstrapId);
        
        IdGenerator idGenerator = new IdGenerator(random);
        Id selfId = idGenerator.generate(bootstrapId.getLimitAsByteArray());
        
        Pointer<A> basePtr = new Pointer(selfId, selfAddress);
        
        this.bootstrap = bootstrap;
        this.chordState = new ChordState<>(basePtr);
        this.finder = finder;
        
        stage = Stage.INITIAL;
    }

    @Override
    protected Task switchTask(Task prev) {
        if (prev != null && prev.getState() == TaskState.FAILED) {
            setFinished(true);
            return null;
        }
        
        switch (stage) {
            case INITIAL: {
                if (bootstrap == null) {
                    setFinished(false);
                    return null;
                }
                chordState.putFinger(bootstrap);
                Id fingerId = chordState.getExpectedFingerId(0); // 0 = nextFingerIdx

                stage = Stage.POPULATE_FINGERS;
                return new FindSuccessorTask<>(fingerId, chordState, finder); // find successor to self
            }
            case POPULATE_FINGERS: {
                Pointer<A> successor = ((FindSuccessorTask) prev).getResult();
                chordState.putFinger(successor);
                
                nextFingerIdx++;
                if (chordState.getBitCount() == nextFingerIdx) {
                    stage = Stage.GET_PREDECESSOR;
                    return new GetPredecessorTask(chordState.getSuccessor(), finder);
                }
                
                Id fingerId = chordState.getExpectedFingerId(nextFingerIdx);
                return new FindSuccessorTask<>(fingerId, chordState, finder);
            }
            case GET_PREDECESSOR: {
                Pointer<A> predecessor = ((GetPredecessorTask) prev).getResult();
                chordState.setPredecessor(predecessor);
                
                stage = Stage.NOTIFY_SUCCESSOR;
                return new NotifyTask(chordState.getBase(), chordState.getSuccessor(), finder); // successor.pred = me
            }
            case NOTIFY_SUCCESSOR: {
                setFinished(false);
                return null;
            }
            default:
                throw new IllegalStateException();
        }
    }

    private enum Stage {
        INITIAL,
        GET_SUCCESSOR,
        POPULATE_FINGERS,
        GET_PREDECESSOR, // grab the predecessor of your successor and set it as your predecessor
        NOTIFY_SUCCESSOR
    }
}
