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
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.Random;
import org.apache.commons.lang3.Validate;

final class FindSuccessorTask<A> extends AbstractChainedTask {
    private Random random;
    private Id findId;
    private ChordState<A> chordState;
    private Stage stage;
    
    private EndpointFinder<A> finder;
    
    private Pointer<A> result;

    public FindSuccessorTask(Random random, Id findId, ChordState<A> chordState, EndpointFinder<A> finder) {
        Validate.notNull(random);
        Validate.notNull(findId);
        Validate.notNull(chordState);
        Validate.notNull(finder);

        this.random = random;
        this.findId = findId;
        this.chordState = chordState;
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
                stage = Stage.FIND_PREDECESSOR;
                return new FindPredecessorTask(random, findId, chordState, finder);
            }
            case FIND_PREDECESSOR: {
                Pointer<A> pointer = ((FindPredecessorTask) prev).getResult();
                stage = Stage.FIND_SUCCESSOR;
                return new GetSuccessorTask(random, pointer, finder);
            }
            case FIND_SUCCESSOR: {
                result = ((GetSuccessorTask) prev).getResult();
                setFinished(false);
                return null;
            }
            default:
                throw new IllegalArgumentException();
        }
    }

    public Pointer<A> getResult() {
        return result;
    }

    private enum Stage {
        INITIAL,
        FIND_PREDECESSOR,
        FIND_SUCCESSOR
    }

}
