package com.offbynull.peernetic.overlay.chord.tasks;

import com.offbynull.peernetic.actor.helpers.AbstractRequestTask;
import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.overlay.chord.messages.GetClosestPrecedingFinger;
import com.offbynull.peernetic.overlay.chord.messages.GetClosestPrecedingFingerReply;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;

public final class GetClosestPrecedingFingerTask<A> extends AbstractRequestTask {
    
    private Pointer<A> closestPredecessor;
    
    public GetClosestPrecedingFingerTask(Id id, Pointer<A> pointer, EndpointFinder<A> finder) {
        super(new GetClosestPrecedingFinger(id), finder.findEndpoint(pointer.getAddress()));
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
