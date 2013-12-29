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
package com.offbynull.peernetic.rpc.invoke.helpers.invokationchain;

import com.offbynull.peernetic.rpc.invoke.AsyncResultListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.Validate;

/**
 * A helper class to chain async invokations.
 * @author Kasra Faghihi
 */
public final class InvokationChain {
    
    private InvokationChainStepResultHandler resultHandler;
    private InvokationChainStepErrorHandler errorHandler;
    private List<InvokationChainStep> steps;

    /**
     * 
     * @param resultHandler called when an invokation successfully returns a result
     * @param errorHandler called when an invokation throws an exception or has a comm error
     * @param steps steps that perform invokations in the invokation chain
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     * @throws IllegalArgumentException if {@code steps} is empty
     */
    public InvokationChain(InvokationChainStepResultHandler resultHandler,
            InvokationChainStepErrorHandler errorHandler, List<InvokationChainStep> steps) {
        Validate.noNullElements(steps);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, steps.size());
        Validate.notNull(resultHandler);
        Validate.notNull(errorHandler);

        this.resultHandler = resultHandler;
        this.errorHandler = errorHandler;
        this.steps = new ArrayList<>(steps);
    }
    
    /**
     * Starts the async invokation chain.
     */
    public void start() {
        AsyncResultListener<?> asyncResultListener = new AsyncResultListener<Object>() {
            private AtomicInteger idx = new AtomicInteger();

            @Override
            public void invokationReturned(Object object) {
                int stepIndex = idx.getAndIncrement();
                if (!resultHandler.handleResult(steps.get(stepIndex), stepIndex, object)) {
                    return;
                }

                int nextStepIndex = stepIndex + 1;
                if (nextStepIndex == steps.size()) {
                    return;
                }

                steps.get(nextStepIndex).doInvoke(this);
            }

            @Override
            public void invokationThrew(Throwable err) {
                handleError(ErrorType.METHOD_THROW, err);
            }

            @Override
            public void invokationFailed(Object err) {
                handleError(ErrorType.FAILURE, err);
            }
            
            private void handleError(ErrorType errorType, Object error) {
                int stepIndex = idx.get();
                ErrorOperation operation = errorHandler.handleError(steps.get(stepIndex), stepIndex, errorType, error);
                
                switch (operation) {
                    case RETRY: {
                        steps.get(stepIndex).doInvoke(this);
                        break;
                    }
                    case CONTINUE: {
                        int nextStepIndex = idx.incrementAndGet();
                        steps.get(nextStepIndex).doInvoke(this);
                        break;
                    }
                    case STOP: {
                        return;
                    }
                    default:
                        throw new IllegalStateException();
                }
            }
        };
        
        steps.get(0).doInvoke(asyncResultListener);
    }
}
