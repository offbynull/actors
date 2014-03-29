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
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.helpers.RequestManager;
import com.offbynull.peernetic.actor.helpers.RequestManager.IncomingRequestHandler;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class RespondTask<A> implements Task {

    private ChordState<A> state;
    private ChordConfig<A> config;
    
    private RequestManager requestManager;
    
    private List<RouteTask<A>> routeTasks;
    
    private TaskState taskState = TaskState.START;
    

    public RespondTask(ChordState<A> state, ChordConfig<A> config) {
        Validate.notNull(state);
        Validate.notNull(config);
        this.state = state;
        this.requestManager = new RequestManager(config.getRandom());
        this.config = config;
        
        this.routeTasks = new LinkedList<>();
    }

    @Override
    public TaskState getState() {
        return taskState;
    }

    @Override
    public long process(long timestamp, Incoming incoming, PushQueue pushQueue) {
        switch (taskState) {
            case START: {
                requestManager.mapRequestHandler(GetClosestPrecedingFinger.class, new GetClosestPrecedingFingerRequestHandler());
                requestManager.mapRequestHandler(GetPredecessor.class, new GetPredecessorRequestHandler());
                requestManager.mapRequestHandler(GetSuccessor.class, new GetSuccessorRequestHandler());
                requestManager.mapRequestHandler(DumpSuccessors.class, new DumpSuccessorsRequestHandler());
                requestManager.mapRequestHandler(Notify.class, new NotifyRequestHandler());
                taskState = TaskState.PROCESSING;
                return requestManager.process(timestamp, pushQueue);
            }
            case PROCESSING: {
                long nextTimestamp = Long.MAX_VALUE;
                if (incoming != null) {
                    requestManager.incomingMessage(timestamp, incoming);
                    
                    if (incoming.getContent() instanceof RouteRequest) {
                        RouteRequest request = (RouteRequest) incoming.getContent();
                        Endpoint requestEndpoint = incoming.getSource();
                        
                        routeTasks.add(new RouteTask(request, requestEndpoint, state, config));
                    }
                }
                
                Iterator<RouteTask<A>> it = routeTasks.iterator();
                while (it.hasNext()) {
                    RouteTask<A> routeTask = it.next();
                    
                    long nextRouteTimestamp = routeTask.process(timestamp, incoming, pushQueue);
                    nextTimestamp = Math.min(nextTimestamp, nextRouteTimestamp);
                    
                    switch (routeTask.getState()) {
                        case COMPLETED:
                        case FAILED:
                            it.remove();
                            break;
                        default:
                            break;
                    }
                }
                
                long nextReqManagerTimestamp = requestManager.process(timestamp, pushQueue);
                nextTimestamp = Math.min(nextTimestamp, nextReqManagerTimestamp);
                
                
                return nextTimestamp;
            }
            default:
                throw new IllegalStateException();
        }
    }
    
    private final class GetClosestPrecedingFingerRequestHandler implements IncomingRequestHandler<GetClosestPrecedingFinger> {

        @Override
        public Object produceResponse(long timestamp, GetClosestPrecedingFinger request) {
            Pointer<A> result = state.getClosestPreceding(request.getId());
            return new GetClosestPrecedingFingerReply<>(result);
        }
    }
    private final class GetPredecessorRequestHandler implements IncomingRequestHandler<GetPredecessor> {

        @Override
        public Object produceResponse(long timestamp, GetPredecessor request) {
            return new GetPredecessorReply<>(state.getPredecessor());
        }
    }
    private final class GetSuccessorRequestHandler implements IncomingRequestHandler<GetSuccessor> {

        @Override
        public Object produceResponse(long timestamp, GetSuccessor request) {
            return new GetSuccessorReply<>(state.getSuccessor());
        }
    }
    private final class DumpSuccessorsRequestHandler implements IncomingRequestHandler<DumpSuccessors> {

        @Override
        public Object produceResponse(long timestamp, DumpSuccessors request) {
            return new DumpSuccessorsReply<>(state.dumpSuccessorTable());
        }
    }
    private final class NotifyRequestHandler implements IncomingRequestHandler<Notify> {

        @Override
        public Object produceResponse(long timestamp, Notify request) {
            Pointer<A> newPred = request.getPredecessor();
            try {
                state.setPredecessor(newPred);
            } catch (IllegalArgumentException iae) { // NOPMD
                // failed to meet conditions for setting predecessor
            }
            
            config.getListener().stateUpdated("Notify Handled",
                    state.getBase(),
                    state.getPredecessor(),
                    state.dumpFingerTable(),
                    state.dumpSuccessorTable());
            
            return new NotifyReply();
        }
    }
}

