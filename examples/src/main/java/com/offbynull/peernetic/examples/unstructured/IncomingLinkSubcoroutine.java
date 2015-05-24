package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkKeepAliveRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkKeptAliveResponse;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkRequest;
import com.offbynull.peernetic.examples.unstructured.internalmessages.Check;
import java.util.HashSet;
import java.util.Set;
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

        // In a loop -- wait up to 15 seconds for keepalive. If none arrived (or exception), remove incoming link and leave
        try {
            while (true) {
                Check check = new Check();
                ctx.addOutgoingMessage(sourceId, timerAddress.appendSuffix("15000"), check); // check interval

                // Keep reading in msg until keepalive or timeout
                while (true) {
                    cnt.suspend();
                    
                    Object msg = ctx.getIncomingMessage();
                    if (msg == check) { // kill this incominglinksubcoroutine on timeout
                        // stop
                        return null;
                    }

                    if (msg instanceof LinkKeepAliveRequest) { // queue up response and continue to main loop if keep alive
                        Set<Address> links = new HashSet<>();
                        links.addAll(state.getLinks());
                        links.addAll(state.getCachedAddresses());
                        
                        ctx.addOutgoingMessage(ctx.getSource(), new LinkKeptAliveResponse(links));
                        break;
                    }
                }
            }
        } finally {
            state.removeIncomingLink(requesterAddress);
        }
    }

}
