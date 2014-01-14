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

import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.helpers.AbstractChainedTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import org.apache.commons.lang3.Validate;

final class ChordTask<A> extends AbstractChainedTask {

    private ChordConfig<A> config;
    private ChordState<A> state;

    private Stage stage = Stage.INITIAL;

    public ChordTask(ChordConfig<A> config) {
        Validate.notNull(config);
        this.config = config;
    }

    @Override
    protected Task switchTask(long timestamp, Task prev, PushQueue pushQueue) {
        if (prev != null && prev.getState() == TaskState.FAILED) {
            setFinished(true);
            return null;
        }
        
        switch (stage) {
            case INITIAL: {
                InitializeTask<A> initializeTask = new InitializeTask<>(config);
                stage = Stage.INITIALIZE;
                return initializeTask;
            }
            case INITIALIZE: {
                InitializeTask<A> initializeTask = (InitializeTask<A>) prev;
                state = initializeTask.getResult();
                
                config.getListener().stateUpdated("Initialized",
                        state.getBase(),
                        state.getPredecessor(),
                        state.dumpFingerTable(),
                        state.dumpSuccessorTable());
                
                MaintainTask<A> maintainTask = new MaintainTask<>(state, config);
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
