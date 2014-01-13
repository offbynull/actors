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

final class InitializeTask<A> extends AbstractChainedTask {
    private int nextFingerIdx;
    
    private ChordConfig<A> config;
    private ChordState<A> state;
    
    private Stage stage;

    public InitializeTask(ChordConfig<A> config) {
        Validate.notNull(config);

        this.state = new ChordState<>(config.getBase());
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
                Pointer<A> bootstrap = config.getBootstrap();
                
                if (bootstrap == null) {
                    setFinished(false);
                    return null;
                }
                state.putFinger(bootstrap);
                Id fingerId = state.getExpectedFingerId(0); // 0 = nextFingerIdx

                stage = Stage.POPULATE_FINGERS;
                return new FindSuccessorTask<>(fingerId, state, config); // find successor to self
            }
            case POPULATE_FINGERS: {
                Pointer<A> successor = ((FindSuccessorTask) prev).getResult();
                state.putFinger(successor);
                
                nextFingerIdx++;
                if (state.getBitCount() == nextFingerIdx) {
                    stage = Stage.GET_PREDECESSOR;
                    return new GetPredecessorTask(state.getSuccessor(), config);
                }
                
                Id fingerId = state.getExpectedFingerId(nextFingerIdx);
                return new FindSuccessorTask<>(fingerId, state, config);
            }
            case GET_PREDECESSOR: {
                Pointer<A> predecessor = ((GetPredecessorTask) prev).getResult();
                if (predecessor != null) {
                    // if joining a node that has no other connections... this is possible if the node we've joined is the first node in the
                    // chord network... after this step we notify the node we've joined that we're it's predecessor, and it should notify
                    // us (after a few moments) that it's our predecessor
                    try {
                        state.setPredecessor(predecessor);
                    } catch (IllegalArgumentException iae) {
                        // thrown if the new predecessor isn't between our current predecessor and us
                    }
                }
                
                stage = Stage.NOTIFY_SUCCESSOR;
                return new NotifyTask(state, config); // successor.pred = me
            }
            case NOTIFY_SUCCESSOR: {
                setFinished(false);
                return null;
            }
            default:
                throw new IllegalStateException();
        }
    }

    public ChordState<A> getResult() {
        return state;
    }

    private enum Stage {
        INITIAL,
        GET_SUCCESSOR,
        POPULATE_FINGERS,
        GET_PREDECESSOR, // grab the predecessor of your successor and set it as your predecessor
        NOTIFY_SUCCESSOR // notify the successor that we are the new predecessor
    }
}
