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
package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineException;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.core.actor.Context;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableObject;

/**
 * Invokes a {@link Subcoroutine} within its own isolated {@link CoroutineRunner}. Use this class when you don't want calls to
 * {@link Continuation#suspend() } within your subcoroutine to also suspend the caller.
 * <p>
 * This class is not thread-safe / immutable.
 * @author Kasra Faghihi
 * @param <T> return type of subcoroutine
 */
public final class SubcoroutineStepper<T> {

    private final MutableObject<T> retValue;
    private final Coroutine coroutine;
    private final CoroutineRunner runner;
    
    private boolean stillRunning;

    /**
     * Constructs a {@link SubcoroutineStepper}.
     * @param context actor context
     * @param subcoroutine subcoroutine to execute
     * @throws NullPointerException if any argument is {@code null}
     */
    public SubcoroutineStepper(Context context, Subcoroutine<T> subcoroutine) {
        Validate.notNull(context);
        Validate.notNull(subcoroutine);

        this.retValue = new MutableObject<>();
        coroutine = cnt -> {
            T ret = subcoroutine.run(cnt);
            retValue.setValue(ret);
        };
        runner = new CoroutineRunner(coroutine);
        runner.setContext(context);
        
        stillRunning = true;
    }

    /**
     * Runs the backing subcoroutine until its next call to {@link Continuation#suspend() } or until it finishes.
     * @return {@code true} if the backing subcoroutine is still running, {@code false} if it has finished
     * @throws IllegalStateException if the backing subcoroutine has already finished
     * @throws CoroutineException if the subcoroutine encounters an exception
     */
    public boolean step() {
        Validate.validState(stillRunning);
        stillRunning = runner.execute();
        return stillRunning;
    }
    
    /**
     * Gets the result of the subcoroutine.
     * @return result of the subcorutine
     * @throws IllegalStateException if the backing subcoroutine has not finished yet
     */
    public T getResult() {
        Validate.validState(!stillRunning);
        return retValue.getValue();
    }
}
