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
import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.List;
import java.util.Random;

final class DumpSuccessorsTask<A> extends AbstractRequestTask {
    
    private List<Pointer<A>> successors;
    
    public DumpSuccessorsTask(Random random, Pointer<A> pointer, EndpointFinder<A> finder) {
        super(random, new DumpSuccessors(), finder.findEndpoint(pointer.getAddress()));
    }

    @Override
    protected boolean processResponse(Object response) {
        if (!(response instanceof DumpSuccessorsReply)) {
            return false;
        }

        successors = ((DumpSuccessorsReply<A>) response).getSuccessors();
        return true;
    }

    public List<Pointer<A>> getResult() {
        return successors;
    }
    
}
