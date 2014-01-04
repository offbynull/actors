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
package com.offbynull.peernetic.rpc.invoke.helpers.invocationchain;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Builder for {@link InvocationChain}.
 * @author Kasra F
 */
public final class InvocationChainBuilder {
    private List<InvocationChainStep> steps = new ArrayList<>();
    private InvocationChainStepErrorHandler errorHandler = new StopInvocationChainStepErrorHandler();
    private InvocationChainStepResultHandler resultHandler = new ContinueInvocationChainStepResultHandler();

    /**
     * Set the error handler.
     * @param errorHandler error handler
     * @return this
     */
    public InvocationChainBuilder setErrorHandler(InvocationChainStepErrorHandler errorHandler) {
        Validate.notNull(errorHandler);
        this.errorHandler = errorHandler;
        return this;
    }

    /**
     * Set the result handler.
     * @param resultHandler result handler
     * @return this
     */
    public InvocationChainBuilder setResultHandler(InvocationChainStepResultHandler resultHandler) {
        Validate.notNull(resultHandler);
        this.resultHandler = resultHandler;
        return this;
    }

    /**
     * Add an invocation step.
     * @param step invocation step
     * @return this
     */
    public InvocationChainBuilder addStep(InvocationChainStep step) {
        Validate.notNull(step);
        steps.add(step);
        return this;
    }
    
    /**
     * Build {@link InvocationChain}.
     * @return build
     */
    public InvocationChain build() {
        return new InvocationChain(resultHandler, errorHandler, steps);
    }
}
