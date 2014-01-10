package com.offbynull.peernetic.overlay.chord.tasks;

import com.offbynull.peernetic.actor.helpers.AbstractRequestTask;
import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.overlay.chord.messages.GetSuccessor;
import com.offbynull.peernetic.overlay.common.id.Pointer;

public final class GetSuccessorTask<A> extends AbstractRequestTask {
    
    private Pointer<A> successor;
    
    public GetSuccessorTask(Pointer<A> pointer, EndpointFinder<A> finder) {
        super(new GetSuccessor(), finder.findEndpoint(pointer.getAddress()));
    }

    @Override
    protected boolean processResponse(Object response) {
        if (!(response instanceof Pointer)) {
            return false;
        }

        successor = (Pointer<A>) response;
        return true;
    }

    public Pointer<A> getResult() {
        return successor;
    }
    
}
