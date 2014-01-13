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
import com.offbynull.peernetic.overlay.chord.ChordOverlayListener.FailureMode;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class StabilizeTask<A> extends AbstractChainedTask {
    private ChordState<A> state;
    private ChordConfig<A> config;
    
    private Pointer<A> revisedSuccessor;

    private Stage stage = Stage.INITIAL;

    public StabilizeTask(ChordState<A> state, ChordConfig<A> config) {
        Validate.notNull(state);
        Validate.notNull(config);
        
        this.state = state;
        this.config = config;
    }

    @Override
    protected Task switchTask(long timestamp, Task prev) {
//        if (prev != null && prev.getState() == TaskState.FAILED) {
//            setFinished(true);
//            return null;
//        }
        
        switch (stage) {
            case INITIAL: {
                Pointer<A> successor = state.getSuccessor();
                
                if (successor.equals(state.getBase())) {
                    setFinished(false);
                    return null;
                }
                
                stage = Stage.CHECK_SUCCESSOR;
                return new GetPredecessorTask<>(successor, config);
            }
            case CHECK_SUCCESSOR: {
                if (prev.getState() == TaskState.FAILED) {
                    // successor did not answer us, shift to next successor in the list and try again
                    try {
                        state.shiftSuccessor();
                    } catch (IllegalStateException ise) {
                        // no more successors available... this node has failed.
                        config.getListener().failed(FailureMode.SUCCESSOR_TABLE_DEPLETED);
                        throw ise;
                    }
                    
                    Pointer<A> successor = state.getSuccessor();
                    // stage = Stage.CHECK_SUCCESSOR; // already in this stage, no point in switching
                    return new GetPredecessorTask<>(successor, config);
                }
                
                Pointer<A> result = ((GetPredecessorTask<A>) prev).getResult();
                
                Id selfId = state.getBaseId();
                Id currSuccessorId = state.getSuccessor().getId();
                if (result != null && result.getId().isWithin(selfId, false, currSuccessorId, true)) {
                    // result can be null if node's predecessor wasn't set yet
                    
                    // if what was given back is the same or closer than our current succesosr, ask for a dump out its successor table
                    // before setting it and notifying it
                    revisedSuccessor = result;
                    stage = Stage.UPDATE_SUCCESSOR;
                    return new DumpSuccessorsTask<>(revisedSuccessor, config);
                } else {
                    // if what was given back is farther away than our current successor, keep what we have and ask it for a dump of its
                    // successor table before setting it and notifying it
                    revisedSuccessor = state.getSuccessor();
                    stage = Stage.UPDATE_SUCCESSOR;
                    return new DumpSuccessorsTask<>(revisedSuccessor, config);
                }
            }
            case UPDATE_SUCCESSOR: {
                if (prev.getState() == TaskState.FAILED) {
                    setFinished(true);
                    return null;
                }
                
                List<Pointer<A>> result = ((DumpSuccessorsTask<A>) prev).getResult();
                state.setSuccessor(revisedSuccessor, result);
                
                stage = Stage.NOTIFY;
                return new NotifyTask<>(state, config);
            }
            case NOTIFY: {
                setFinished(prev.getState() == TaskState.FAILED);
                return null;
            }
            default:
                throw new IllegalStateException();
        }
    }
    
    private enum Stage {
        INITIAL,
        CHECK_SUCCESSOR,
        UPDATE_SUCCESSOR,
        NOTIFY,
    }
}
