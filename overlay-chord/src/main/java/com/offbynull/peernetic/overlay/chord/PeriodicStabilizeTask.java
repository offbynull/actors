package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.helpers.AbstractPeriodicTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import java.util.Random;
import org.apache.commons.lang3.Validate;

final class PeriodicStabilizeTask<A> extends AbstractPeriodicTask {

    private Random random;
    private ChordState<A> chordState;
    private EndpointFinder<A> finder;
    private ChordOverlayListener<A> listener;

    public PeriodicStabilizeTask(long waitDuration, Random random, ChordState<A> chordState, EndpointFinder<A> finder,
            ChordOverlayListener<A> listener) {
        super(waitDuration);
        Validate.notNull(random);
        Validate.notNull(chordState);
        Validate.notNull(finder);
        Validate.notNull(listener);
        
        this.random = random;
        this.chordState = chordState;
        this.finder = finder;
        this.listener = listener;
    }

    @Override
    protected Task startTask() {
        return new StabilizeTask(random, chordState, finder, listener);
    }
    
}
