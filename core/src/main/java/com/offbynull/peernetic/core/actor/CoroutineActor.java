/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.core.actor;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import org.apache.commons.lang3.Validate;

public final class CoroutineActor implements Actor {
    private CoroutineRunner coroutineRunner;
    private boolean executing;

    public CoroutineActor(Coroutine task) {
        Validate.notNull(task);
        coroutineRunner = new CoroutineRunner(task);
        executing = true;
    }

    @Override
    public boolean onStep(Context context) throws Exception {
        // if continuation has ended, ignore any further messages
        if (executing) {
            coroutineRunner.setContext(context); // set once
            executing = coroutineRunner.execute();
        }
        
        return executing;
    }

    public boolean isFinished() {
        return executing;
    }
    
}
