package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.helpers.AbstractChainedTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.offbynull.peernetic.overlay.chord.tasks.InitializeTask;
import com.offbynull.peernetic.overlay.chord.tasks.MaintainTask;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.Random;
import org.apache.commons.lang3.Validate;

final class ChordTask<A> extends AbstractChainedTask {

    private Pointer<A> self;
    private Pointer<A> bootstrap;
    private EndpointFinder<A> finder;
    private Random random;
    
    private ChordState<A> chordState;

    private Stage stage = Stage.INITIAL;

    public ChordTask(Pointer<A> self, Pointer<A> bootstrap, Random random, EndpointFinder<A> finder) {
        Validate.notNull(self);
        Validate.notNull(bootstrap);
        Validate.notNull(random);
        Validate.notNull(finder);
        
        this.self = self;
        this.bootstrap = bootstrap;
        this.random = random;
        this.finder = finder;
    }

    @Override
    protected Task switchTask(Task prev) {
        if (prev != null && prev.getState() == TaskState.FAILED) {
            setFinished(true);
            return null;
        }
        
        switch (stage) {
            case INITIAL: {
                InitializeTask<A> initializeTask = new InitializeTask<>(self, bootstrap, finder);
                stage = Stage.INITIALIZE;
                return initializeTask;
            }
            case INITIALIZE: {
                InitializeTask<A> initializeTask = (InitializeTask<A>) prev;
                chordState = initializeTask.getResult();
                
                MaintainTask<A> maintainTask = new MaintainTask<>(chordState, random);
                stage = Stage.MAINTAIN;
                
                return maintainTask;
            }
//            case MAINTAIN: {
//                break;
//            }
            default:
                throw new IllegalStateException();
        }
    }

    private enum Stage {

        INITIAL,
        INITIALIZE,
        MAINTAIN
    }
}
