/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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

import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.helpers.AbstractChainedTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.Random;
import org.apache.commons.lang3.Validate;

final class FixFingerTask<A> extends AbstractChainedTask {
    
    private Random random;
    private ChordState<A> chordState;
    private int idx;
    
    private EndpointFinder<A> finder;

    private Stage stage = Stage.INITIAL;

    public FixFingerTask(Random random, ChordState<A> chordState, EndpointFinder<A> finder, int idx) {
        Validate.notNull(random);
        Validate.notNull(chordState);
        Validate.notNull(finder);
        Validate.inclusiveBetween(1, chordState.getBitCount(), idx); // cannot be 0
        
        this.random = random;
        this.chordState = chordState;
        this.finder = finder;
        this.idx = idx;
    }
    
    

    @Override
    protected Task switchTask(long timestamp, Task prev) {
        if (prev != null && prev.getState() == TaskState.FAILED) {
            setFinished(true);
            return null;
        }
        
        switch (stage) {
            case INITIAL: {
                stage = Stage.FIND_SUCCESSOR;
                return new FindSuccessorTask(random, chordState.getExpectedFingerId(idx), chordState, finder);
            }
            case FIND_SUCCESSOR: {
                Pointer<A> result = ((FindSuccessorTask) prev).getResult();
                
                Pointer<A> self = chordState.getBase();
                if (result.equals(self)) {
                    // if we got back ourself then remove the finger (this ensures that the fingertable will reset to us), unless it's
                    // already set to us (because there would be no point in resetting it at that point)
                    Pointer<A> fingerAtIdx = chordState.getFinger(idx);
                    if (fingerAtIdx.equals(self)) {
                        chordState.removeFinger(fingerAtIdx);
                    }
                } else {
                    chordState.putFinger(result);
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
