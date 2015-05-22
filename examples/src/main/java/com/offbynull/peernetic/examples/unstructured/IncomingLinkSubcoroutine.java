package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkKeepAliveRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkRequest;
import com.offbynull.peernetic.examples.unstructured.internalmessages.Check;
import org.apache.commons.lang3.Validate;

final class IncomingLinkSubcoroutine implements Subcoroutine<Void> {

    private final Address sourceId;
    private final Address timerAddress;
    private final State state;

    public IncomingLinkSubcoroutine(Address sourceId, Address timerAddress, State state) {
        Validate.notNull(sourceId);
        Validate.notNull(timerAddress);
        Validate.notNull(state);
        this.sourceId = sourceId;
        this.timerAddress = timerAddress;
        this.state = state;
    }

    @Override
    public Address getId() {
        return sourceId;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        // Validate initial msg is a link request
        Validate.isTrue(ctx.getIncomingMessage() instanceof LinkRequest);
        Address requesterAddress = ctx.getSource();

        try {
            // In a loop -- wait up to 15 seconds for keepalive. If none arrived (or exception), remove incoming link and leave
            while (true) {
                Check check = new Check();
                ctx.addOutgoingMessage(timerAddress.appendSuffix("15000"), check); // check interval
                cnt.suspend();

                Object msg = ctx.getIncomingMessage();
                if (msg == check || !(msg instanceof LinkKeepAliveRequest)) {
                    return null;
                }
            }
        } finally {
            state.removeIncomingLink(requesterAddress);
        }
    }

}
