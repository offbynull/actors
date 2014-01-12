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
import com.offbynull.peernetic.overlay.common.id.IdUtils;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.Random;
import org.apache.commons.lang3.Validate;

final class ChordTask<A> extends AbstractChainedTask {

    private Pointer<A> self;
    private Pointer<A> bootstrap;
    private EndpointFinder<A> finder;
    private Random random;
    
    private ChordState<A> chordState;
    private ChordOverlayListener<A> listener;

    private Stage stage = Stage.INITIAL;

    public ChordTask(Pointer<A> self, Pointer<A> bootstrap, Random random, EndpointFinder<A> finder, ChordOverlayListener<A> listener) {
        Validate.notNull(self);
        Validate.notNull(random);
        Validate.notNull(finder);
        if (bootstrap != null) {
            Validate.isTrue(self.getId().getLimitAsBigInteger().equals(bootstrap.getId().getLimitAsBigInteger()));
        }
        IdUtils.ensureLimitPowerOfTwo(self);
        Validate.notNull(listener);
        
        this.self = self;
        this.bootstrap = bootstrap;
        this.random = random;
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
                InitializeTask<A> initializeTask = new InitializeTask<>(random, self, bootstrap, finder);
                stage = Stage.INITIALIZE;
                return initializeTask;
            }
            case INITIALIZE: {
                InitializeTask<A> initializeTask = (InitializeTask<A>) prev;
                chordState = initializeTask.getResult();
                
                listener.stateUpdated("Initialized",
                        chordState.getBase(),
                        chordState.getPredecessor(),
                        chordState.dumpFingerTable(),
                        chordState.dumpSuccessorTable());
                
                MaintainTask<A> maintainTask = new MaintainTask<>(random, chordState, finder, listener);
                stage = Stage.MAINTAIN;
                
                return maintainTask;
            }
//            case MAINTAIN: {
//                break;
//            }
            default:
                throw new IllegalStateException();
        }
    }

    private enum Stage {

        INITIAL,
        INITIALIZE,
        MAINTAIN
    }
}
