package com.offbynull.peernetic.actor.helpers;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.helpers.RequestManager.OutgoingRequestHandler;
import org.apache.commons.lang3.Validate;

public abstract class AbstractRequestTask implements Task {
    private Object request;
    private Endpoint destination;
    private TaskState taskState;
    
    private RequestManager requestManager;
    
    private InternalOutgoingRequestHandler internalOutgoingRequestHandler;
    
    public AbstractRequestTask(Object request, Endpoint destination) {
        Validate.notNull(request);
        Validate.notNull(destination);

        this.request = request;
        this.destination = destination;
        
        taskState = TaskState.START;
    }
    
    @Override
    public long process(long timestamp, Incoming incoming, PushQueue pushQueue) {
        switch (taskState) {
            case START: {
                start(timestamp);
                break;
            }
            case PROCESSING: {
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

        requestManager.newRequest(timestamp, request, destination, internalOutgoingRequestHandler, 10000L, 3);
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
