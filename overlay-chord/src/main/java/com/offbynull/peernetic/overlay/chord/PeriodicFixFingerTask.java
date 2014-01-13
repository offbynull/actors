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

import com.offbynull.peernetic.actor.helpers.AbstractPeriodicTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import org.apache.commons.lang3.Validate;

final class PeriodicFixFingerTask<A> extends AbstractPeriodicTask {

    private ChordState<A> state;
    private ChordConfig<A> config;
    
    private int fingerIdx = 1;

    public PeriodicFixFingerTask(ChordState<A> state, ChordConfig<A> config) {
        super(config.getFixFingerPeriod());
        Validate.notNull(state);
        Validate.notNull(config);
        
        this.state = state;
        this.config = config;
    }
    
    @Override
    protected Task startTask() {
        fingerIdx++;
        if (fingerIdx >= state.getBitCount()) {
            fingerIdx = 1;
        }
        return new FixFingerTask<>(state, config, fingerIdx);
    }
}
