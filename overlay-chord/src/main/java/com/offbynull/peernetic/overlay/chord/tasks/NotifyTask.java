package com.offbynull.peernetic.overlay.chord.tasks;

import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.helpers.AbstractRequestTask;
import com.offbynull.peernetic.overlay.chord.messages.Notify;
import com.offbynull.peernetic.overlay.chord.messages.NotifyReply;
import com.offbynull.peernetic.overlay.common.id.Pointer;

public final class NotifyTask<A> extends AbstractRequestTask {
    public NotifyTask(Pointer<A> self, Pointer<A> successor, EndpointFinder<A> finder) {
        super(new Notify<>(self), finder.findEndpoint(successor.getAddress()));
    }

    @Override
    protected boolean processResponse(Object response) {
        return response instanceof NotifyReply;
    }
}
