package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.helpers.AbstractPeriodicTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import java.util.Random;
import org.apache.commons.lang3.Validate;

final class PeriodicFixFingerTask<A> extends AbstractPeriodicTask {

    private Random random;
    private ChordState<A> chordState;
    private EndpointFinder<A> finder;
    
    private int fingerIdx = 1;

    public PeriodicFixFingerTask(long waitDuration, Random random, ChordState<A> chordState, EndpointFinder<A> finder) {
        super(waitDuration);
        Validate.notNull(random);
        Validate.notNull(chordState);
        Validate.notNull(finder);
        
        this.random = random;
        this.chordState = chordState;
        this.finder = finder;
    }
    
    @Override
    protected Task startTask() {
        fingerIdx++;
        if (fingerIdx >= chordState.getBitCount()) {
            fingerIdx = 1;
        }
                
        return new FixFingerTask(random, chordState, finder, fingerIdx);
    }
}
