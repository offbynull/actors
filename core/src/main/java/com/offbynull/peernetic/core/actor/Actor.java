/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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

final class Actor {
    private CoroutineRunner coroutineRunner;
    private boolean executing;

    /**
     * Constructs a {@link CoroutineActor} object.
     * @param task coroutine to delegate to
     * @throws NullPointerException if any argument is {@code null}
     */
    public Actor(Coroutine task) {
        Validate.notNull(task);
        coroutineRunner = new CoroutineRunner(task);
        executing = true;
    }

    public boolean onStep(Context context) throws Exception {
        // if continuation has ended, ignore any further messages
        if (executing) {
            coroutineRunner.setContext(context); // set once
            executing = coroutineRunner.execute();
        }
        
        return executing;
    }
    
}
