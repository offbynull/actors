package com.offbynull.peernetic.overlay.chord.tasks;

import com.offbynull.peernetic.actor.helpers.AbstractRequestTask;
import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.overlay.chord.messages.SetPredecessor;
import com.offbynull.peernetic.overlay.chord.messages.SetPredecessorReply;
import com.offbynull.peernetic.overlay.common.id.Pointer;

public final class SetPredecessorTask<A> extends AbstractRequestTask {
    
    public SetPredecessorTask(Pointer<A> pointer, Pointer<A> self, EndpointFinder<A> finder) {
        super(new SetPredecessor<>(self), finder.findEndpoint(pointer.getAddress()));
    }

    @Override
    protected boolean processResponse(Object response) {
        if (!(response instanceof SetPredecessorReply)) {
            return false;
        }

        return true;
    }
}
