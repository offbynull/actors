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

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.helpers.AbstractChainedTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import org.apache.commons.lang3.Validate;

final class RouteTask<A> extends AbstractChainedTask {

    private ChordState<A> state;
    private ChordConfig<A> config;
    private Stage stage = Stage.START;
    
    private RouteRequest request;
    private Endpoint requestEndpoint;
    
    public RouteTask(RouteRequest request, Endpoint requestEndpoint, ChordState<A> state, ChordConfig<A> config) {
        Validate.notNull(request);
        Validate.notNull(requestEndpoint);
        Validate.notNull(state);
        Validate.notNull(config);

        this.request = request;
        this.requestEndpoint = requestEndpoint;
        this.state = state;
        this.config = config;
        
        stage = Stage.START;
    }

    @Override
    protected Task switchTask(long timestamp, Task prev, PushQueue pushQueue) {
        if (prev != null && prev.getState() == TaskState.FAILED) {
            setFinished(true);
            return null;
        }

        switch (stage) {
            case START: {
                stage = Stage.ROUTING;
                return new FindSuccessorTask(request.getDestination(), state, config);
            }
            case ROUTING: {
                Pointer<A> result = ((FindSuccessorTask<A>) prev).getResult();
                pushQueue.push(requestEndpoint, new RouteResponse<>(result));
                setFinished(false);
                return null;
            }
            default:
                throw new IllegalStateException();
        }
    }

    private enum Stage {

        START,
        ROUTING
    }
}
