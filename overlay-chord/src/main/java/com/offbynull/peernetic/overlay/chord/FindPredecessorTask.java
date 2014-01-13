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
import com.offbynull.peernetic.overlay.chord.core.RouteResult;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import org.apache.commons.lang3.Validate;

final class FindPredecessorTask<A> extends AbstractChainedTask {    
    private Id findId;
    private ChordState<A> state;
    private ChordConfig<A> config;
    
    private Stage stage = Stage.INITIAL;
    
    private Pointer<A> lastClosestPredecessor;
    
    private Pointer<A> result;

    public FindPredecessorTask(Id findId, ChordState<A> state, ChordConfig<A> config) {
        Validate.notNull(findId);
        Validate.notNull(state);
        Validate.notNull(config);
        
        this.findId = findId;
        this.state = state;
        this.config = config;
    }
    
    public Pointer<A> getResult() {
        return result;
    }

    @Override
    protected Task switchTask(long timestamp, Task prev) {
        if (prev != null && prev.getState() == TaskState.FAILED) {
            setFinished(true);
            return null;
        }
        
        
        switch (stage) {
            case INITIAL: {
                if (findId.isWithin(state.getBaseId(), false, state.getSuccessor().getId(), true)) {
                    result = state.getBase();
                    setFinished(false);
                    return null;
                }
                
                RouteResult<A> routeResult = state.route(findId);

                stage = Stage.FINDING_LAST_CLOSEST_PREDECESSOR;
                return new GetClosestPrecedingFingerTask(findId, routeResult.getPointer(), config);
            }
            case FINDING_LAST_CLOSEST_PREDECESSOR: {
                lastClosestPredecessor = ((GetClosestPrecedingFingerTask) prev).getResult();
                stage = Stage.FINDING_LAST_CLOSEST_PREDECESSORS_SUCCESSOR;
                return new GetSuccessorTask(lastClosestPredecessor, config);
            }
            case FINDING_LAST_CLOSEST_PREDECESSORS_SUCCESSOR: {
                Pointer<A> successor = ((GetSuccessorTask<A>) prev).getResult();

                Id lastClosestPredId = lastClosestPredecessor.getId();
                Id successorId = successor.getId();

                if (findId.isWithin(lastClosestPredId, false, successorId, true)) {
                    stage = Stage.FINDING_LAST_CLOSEST_PREDECESSOR;
                    return new GetClosestPrecedingFingerTask(findId, lastClosestPredecessor, config);
                }

                setFinished(false);
                result = lastClosestPredecessor;
                return null;
            }
            default:
                throw new IllegalStateException();
        }
    }
    
    private enum Stage {
        INITIAL,
        FINDING_LAST_CLOSEST_PREDECESSOR,
        FINDING_LAST_CLOSEST_PREDECESSORS_SUCCESSOR
    }
}
