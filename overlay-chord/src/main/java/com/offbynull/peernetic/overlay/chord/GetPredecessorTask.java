/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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

import com.offbynull.peernetic.actor.helpers.AbstractRequestTask;
import com.offbynull.peernetic.overlay.common.id.Pointer;

final class GetPredecessorTask<A> extends AbstractRequestTask {
    
    private Pointer<A> predecessor;
    
    public GetPredecessorTask(Pointer<A> pointer, ChordConfig<A> config) {
        super(config.getRandom(),
                new GetPredecessor(),
                config.getFinder().findEndpoint(pointer.getAddress()),
                config.getRpcTimeoutDuration(),
                config.getRpcMaxSendAttempts());
    }

    @Override
    protected boolean processResponse(Object response) {
        if (!(response instanceof GetPredecessorReply)) {
            return false;
        }

        predecessor = ((GetPredecessorReply<A>) response).getPredecessor();
        return true;
    }

    public Pointer<A> getResult() {
        return predecessor;
    }
    
}
