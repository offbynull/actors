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
import com.offbynull.peernetic.overlay.common.id.IdUtils;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.Random;
import org.apache.commons.lang3.Validate;

final class InitializeTask<A> extends AbstractChainedTask {
    private int nextFingerIdx;
    
    private Random random;
    private Pointer<A> bootstrap;
    private Stage stage;
    
    private EndpointFinder<A> finder;

    private ChordState<A> chordState;

    public InitializeTask(Random random, Pointer<A> self, Pointer<A> bootstrap, EndpointFinder<A> finder) {
        Validate.notNull(random);
        Validate.notNull(self);
        Validate.notNull(finder);

        IdUtils.ensureLimitPowerOfTwo(self.getId());
        if (bootstrap != null) {
            Validate.isTrue(bootstrap.getId().getLimitAsBigInteger().equals(self.getId().getLimitAsBigInteger()));
        }
        
        this.random = random;
        this.bootstrap = bootstrap;
        this.chordState = new ChordState<>(self);
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
                if (bootstrap == null) {
                    setFinished(false);
                    return null;
                }
                chordState.putFinger(bootstrap);
                Id fingerId = chordState.getExpectedFingerId(0); // 0 = nextFingerIdx

                stage = Stage.POPULATE_FINGERS;
                return new FindSuccessorTask<>(random, fingerId, chordState, finder); // find successor to self
            }
            case POPULATE_FINGERS: {
                Pointer<A> successor = ((FindSuccessorTask) prev).getResult();
                chordState.putFinger(successor);
                
                nextFingerIdx++;
                if (chordState.getBitCount() == nextFingerIdx) {
                    stage = Stage.GET_PREDECESSOR;
                    return new GetPredecessorTask(random, chordState.getSuccessor(), finder);
                }
                
                Id fingerId = chordState.getExpectedFingerId(nextFingerIdx);
                return new FindSuccessorTask<>(random, fingerId, chordState, finder);
            }
            case GET_PREDECESSOR: {
                Pointer<A> predecessor = ((GetPredecessorTask) prev).getResult();
                chordState.setPredecessor(predecessor);
                
                stage = Stage.NOTIFY_SUCCESSOR;
                return new NotifyTask(random, chordState.getBase(), chordState.getSuccessor(), finder); // successor.pred = me
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
        return chordState;
    }

    private enum Stage {
        INITIAL,
        GET_SUCCESSOR,
        POPULATE_FINGERS,
        GET_PREDECESSOR, // grab the predecessor of your successor and set it as your predecessor
        NOTIFY_SUCCESSOR // notify the successor that we are the new predecessor
    }
}
