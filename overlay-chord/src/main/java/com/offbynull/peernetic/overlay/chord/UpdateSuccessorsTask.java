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
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.Validate;

final class UpdateSuccessorsTask<A> extends AbstractChainedTask {
    
    private Random random;
    private ChordState<A> chordState;
    
    private EndpointFinder<A> finder;

    ChordOverlayListener<A> listener;
    
    private Stage stage = Stage.INITIAL;

    public UpdateSuccessorsTask(Random random, ChordState<A> chordState, EndpointFinder<A> finder, ChordOverlayListener<A> listener) {
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
    protected Task switchTask(long timestamp, Task prev) {
        if (prev != null && prev.getState() == TaskState.FAILED) {
            setFinished(true);
            return null;
        }
        
        switch (stage) {
            case INITIAL: {
                stage = Stage.DUMP_SUCCESSORS;
                return new DumpSuccessorsTask<>(random, chordState.getSuccessor(), finder);
            }
            case DUMP_SUCCESSORS: {
                List<Pointer<A>> result = ((DumpSuccessorsTask<A>) prev).getResult();
                chordState.setSuccessor(chordState.getSuccessor(), result);
                
                listener.stateUpdated("Successors updated",
                        chordState.getBase(),
                        chordState.getPredecessor(),
                        chordState.dumpFingerTable(),
                        chordState.dumpSuccessorTable());
                
                setFinished(false);
                return null;
            }
            default:
                throw new IllegalStateException();
        }
    }
    
    private enum Stage {
        INITIAL,
        DUMP_SUCCESSORS
    }
}
