/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.helpers.AbstractChainedTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import org.apache.commons.lang3.Validate;

final class FixFingerTask<A> extends AbstractChainedTask {
    
    private ChordState<A> state;
    private ChordConfig<A> config;
    
    private int idx;

    private Stage stage = Stage.INITIAL;

    public FixFingerTask(ChordState<A> state, ChordConfig<A> config, int idx) {
        Validate.notNull(state);
        Validate.notNull(config);
        Validate.inclusiveBetween(1, state.getBitCount(), idx); // cannot be 0
        
        this.state = state;
        this.config = config;
        this.idx = idx;
    }
    
    

    @Override
    protected Task switchTask(long timestamp, Task prev, PushQueue pushQueue) {
        if (prev != null && prev.getState() == TaskState.FAILED) {
            setFinished(true);
            return null;
        }
        
        switch (stage) {
            case INITIAL: {
                stage = Stage.FIND_SUCCESSOR;
                return new FindSuccessorTask(state.getExpectedFingerId(idx), state, config);
            }
            case FIND_SUCCESSOR: {
                Pointer<A> result = ((FindSuccessorTask) prev).getResult();
                
                Pointer<A> self = state.getBase();
                if (result.equals(self)) {
                    // if we got back ourself then remove the finger (this ensures that the fingertable will reset to us), unless it's
                    // already set to us (because there would be no point in resetting it at that point)
                    Pointer<A> fingerAtIdx = state.getFinger(idx);
                    if (!fingerAtIdx.equals(self)) {
                        state.removeFinger(fingerAtIdx);
                    }
                } else {
                    state.putFinger(result);
                    
                    config.getListener().stateUpdated("Finger Updated " + idx,
                            state.getBase(),
                            state.getPredecessor(),
                            state.dumpFingerTable(),
                            state.dumpSuccessorTable());
                }
                
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
