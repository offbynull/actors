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
import com.offbynull.peernetic.actor.helpers.AbstractMultiTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang3.Validate;

final class MaintainTask<A> extends AbstractMultiTask {

    private ChordState<A> chordState;
    private Random random;
    private EndpointFinder<A> finder;
    private ChordOverlayListener<A> listener;

    public MaintainTask(Random random, ChordState<A> chordState, EndpointFinder<A> finder, ChordOverlayListener<A> listener) {
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
    protected Set<Task> taskStateUpdated(Set<Task> finished) {
        for (Task task : finished) {
            if (task.getState() == TaskState.FAILED) {
                setFinished(true);
                return Collections.emptySet();
            }
        }
     
        Set<Task> newTasks = new HashSet<>();
        if (finished.isEmpty()) { // initial call
            newTasks.add(new RespondTask(random, chordState, listener));
            newTasks.add(new PeriodicStabilizeTask<>(3000L, random, chordState, finder, listener));
            newTasks.add(new PeriodicFixFingerTask<>(3000L, random, chordState, finder));
            newTasks.add(new PeriodicUpdateSuccessorsTask<>(3000L, random, chordState, finder, listener));
        }
        
        return newTasks;
    }
    
}
