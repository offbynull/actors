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
package com.offbynull.peernetic.actor.helpers;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.helpers.RequestManager.OutgoingRequestHandler;
import java.util.Random;
import org.apache.commons.lang3.Validate;

/**
 * Encapsulates a request/response sequence as a task. If response arrives, {@link #processResponse(java.lang.Object) } is called. If
 * response times out, the task is marked as {@link TaskState#FAILED}.
 * @author Kasra Faghihi
 */
public abstract class AbstractRequestTask implements Task {
    private Object request;
    private Endpoint destination;
    private TaskState taskState;
    
    private long timeout;
    private int maxAttempts;
    
    private RequestManager requestManager;
    
    private InternalOutgoingRequestHandler internalOutgoingRequestHandler;
    
    /**
     * Constructs a {@link AbstractRequestTask} object.
     * @param random random for internal request manager
     * @param request request to push out
     * @param destination destination to push request to
     * @param timeout timeout per attempt
     * @param maxAttempts maximum number of attempts
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any numeric argument is non-positive ({@code <= 0})
     */
    public AbstractRequestTask(Random random, Object request, Endpoint destination, long timeout, int maxAttempts) {
        Validate.notNull(random);
        Validate.notNull(request);
        Validate.notNull(destination);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, maxAttempts);

        this.request = request;
        this.destination = destination;
        this.requestManager = new RequestManager(random);
        this.timeout = timeout;
        this.maxAttempts = maxAttempts;
        
        taskState = TaskState.START;
    }
    
    @Override
    public final long process(long timestamp, Incoming incoming, PushQueue pushQueue) {
        switch (taskState) {
            case START: {
                start(timestamp);
                break;
            }
            case PROCESSING: {
                if (incoming != null) {
                    requestManager.incomingMessage(timestamp, incoming);
                }
                queryHandler();
                break;
            }
            case FAILED:
            case COMPLETED: {
                break;
            }
            default:
                throw new IllegalStateException();
        }
        
        return requestManager.process(timestamp, pushQueue);
    }
    
    private void start(long timestamp) {
        internalOutgoingRequestHandler = new InternalOutgoingRequestHandler();

        requestManager.newRequest(timestamp, request, destination, internalOutgoingRequestHandler, timeout, maxAttempts);
        taskState = TaskState.PROCESSING;
    }
    
    private void queryHandler() {
        switch (internalOutgoingRequestHandler.getState()) {
            case PROCESSING:
                break;
            case SUCCESSFUL:
                taskState = TaskState.COMPLETED;
                break;
            case FAILED:
                taskState = TaskState.FAILED;
                break;
            default:
                throw new IllegalStateException();
        }
    }
    
    @Override
    public final TaskState getState() {
        return taskState;
    }
    
    /**
     * Called once response arrives.
     * @param response response object
     * @return {@code false} to set put this task in to a {@link TaskState#FAILED} state, {@code true} for {@link TaskState#COMPLETED}
     * state.
     */
    protected abstract boolean processResponse(Object response);
    
    private final class InternalOutgoingRequestHandler implements OutgoingRequestHandler<Object> {

        private HandlerState state = HandlerState.PROCESSING;
        
        @Override
        public void responseComplete(Object request, Object response) {
            state = processResponse(response) ? HandlerState.SUCCESSFUL : HandlerState.FAILED;
        }

        @Override
        public void responseTimedOut(Object request) {
            state = HandlerState.FAILED;
        }

        public HandlerState getState() {
            return state;
        }
    }
    
    private enum HandlerState {

        PROCESSING,
        SUCCESSFUL,
        FAILED,
    }
}
