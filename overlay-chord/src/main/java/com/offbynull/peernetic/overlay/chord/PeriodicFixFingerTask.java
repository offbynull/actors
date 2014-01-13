package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.actor.helpers.AbstractPeriodicTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import org.apache.commons.lang3.Validate;

final class PeriodicFixFingerTask<A> extends AbstractPeriodicTask {

    private ChordState<A> state;
    private ChordConfig<A> config;
    
    private int fingerIdx = 1;

    public PeriodicFixFingerTask(ChordState<A> state, ChordConfig<A> config) {
        super(config.getNotifyPeriod());
        Validate.notNull(state);
        Validate.notNull(config);
        
        this.state = state;
        this.config = config;
    }
    
    @Override
    protected Task startTask() {
        fingerIdx++;
        if (fingerIdx >= state.getBitCount()) {
            fingerIdx = 1;
        }
                
        return new FixFingerTask<>(state, config, fingerIdx);
    }
}
