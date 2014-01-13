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

import com.offbynull.peernetic.actor.helpers.AbstractChainedTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import org.apache.commons.lang3.Validate;

final class FindSuccessorTask<A> extends AbstractChainedTask {
    private Id findId;
    private ChordState<A> state;
    private ChordConfig<A> config;
    
    private Stage stage;
    
    private Pointer<A> result;

    public FindSuccessorTask(Id findId, ChordState<A> state, ChordConfig<A> config) {
        Validate.notNull(findId);
        Validate.notNull(state);
        Validate.notNull(config);

        this.findId = findId;
        this.state = state;
        this.config = config;
        
        stage = Stage.INITIAL;
    }

    @Override
    protected Task switchTask(long timestamp, Task prev) {
        if (prev != null && prev.getState() == TaskState.FAILED) {
            setFinished(true);
            return null;
        }
        
        switch (stage) {
            case INITIAL: {
                stage = Stage.FIND_PREDECESSOR;
                return new FindPredecessorTask(findId, state, config);
            }
            case FIND_PREDECESSOR: {
                Pointer<A> pointer = ((FindPredecessorTask<A>) prev).getResult();
                
                if (pointer.equals(state.getBase())) {
                    result = state.getSuccessor();
                    setFinished(false);
                    return null;
                } else {
                    stage = Stage.FIND_SUCCESSOR;
                    return new GetSuccessorTask(pointer, config);
                }
            }
            case FIND_SUCCESSOR: {
                result = ((GetSuccessorTask<A>) prev).getResult();
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
