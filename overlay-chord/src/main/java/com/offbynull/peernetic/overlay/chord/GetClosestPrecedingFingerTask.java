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

import com.offbynull.peernetic.actor.helpers.AbstractRequestTask;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;

final class GetClosestPrecedingFingerTask<A> extends AbstractRequestTask {
    
    private Pointer<A> closestPredecessor;
    
    public GetClosestPrecedingFingerTask(Id id, Pointer<A> pointer, ChordConfig<A> config) {
        super(config.getRandom(),
                new GetClosestPrecedingFinger(id),
                config.getFinder().findEndpoint(pointer.getAddress()),
                config.getRpcTimeoutDuration(),
                config.getRpcMaxSendAttempts());
    }

    @Override
    protected boolean processResponse(Object response) {
        if (!(response instanceof GetClosestPrecedingFingerReply)) {
            return false;
        }

        closestPredecessor = ((GetClosestPrecedingFingerReply<A>) response).getClosestPredecessor();
        return true;
    }

    public Pointer<A> getResult() {
        return closestPredecessor;
    }
    
}
