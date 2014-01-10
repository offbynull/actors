package com.offbynull.peernetic.overlay.chord.tasks;

import com.offbynull.peernetic.actor.helpers.AbstractRequestTask;
import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.overlay.chord.messages.GetPredecessor;
import com.offbynull.peernetic.overlay.common.id.Pointer;

public final class GetPredecessorTask<A> extends AbstractRequestTask {
    
    private Pointer<A> predecessor;
    
    public GetPredecessorTask(Pointer<A> pointer, EndpointFinder<A> finder) {
        super(new GetPredecessor(), finder.findEndpoint(pointer.getAddress()));
    }

    @Override
    protected boolean processResponse(Object response) {
        if (!(response instanceof Pointer)) {
            return false;
        }

        predecessor = (Pointer<A>) response;
        return true;
    }

    public Pointer<A> getResult() {
        return predecessor;
    }
    
}
