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

/**
 * Handles async invokation results.
 * @author Kasra Faghihi
 */
public interface InvokationChainStepResultHandler {

    /**
     * Handles async invokation results.
     * @param step step that the result is for
     * @param stepIndex index of {@code step}
     * @param result result
     * @return {@code true} to continue the invokation chain, {@code false} to stop
     * @throws NullPointerException if any arguments other than {@code result} are {@code null}
     * @throws IllegalArgumentException if any numeric arguments are negative
     */
    boolean handleResult(InvokationChainStep step, int stepIndex, Object result);
    
}
