package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.info;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkKeepAliveRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkKeptAliveResponse;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkRequest;
import com.offbynull.peernetic.examples.unstructured.internalmessages.Check;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;

final class IncomingLinkSubcoroutine implements Subcoroutine<Void> {

    private final Address sourceId;
    private final Address timerAddress;
    private final Address logAddress;
    private final State state;

    public IncomingLinkSubcoroutine(Address sourceId, Address timerAddress, Address logAddress, State state) {
        Validate.notNull(sourceId);
        Validate.notNull(timerAddress);
        Validate.notNull(logAddress);
        Validate.notNull(state);
        this.sourceId = sourceId;
        this.timerAddress = timerAddress;
        this.logAddress = logAddress;
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
        
        // remove last element of sourceAddress, as its a temporary identifier used by requestsubcoroutine to identify responses
        // e.g. actor:1:router:out0:7515937758611767298 -> actor:1:router:out0
        requesterAddress = requesterAddress.removeSuffix(1);

        // In a loop -- wait up to 15 seconds for keepalive. If none arrived (or exception), remove incoming link and leave
        try {
            while (true) {
                ctx.addOutgoingMessage(sourceId, logAddress, info("Waiting for keepalive from {}", requesterAddress));
                
                Check check = new Check();
                ctx.addOutgoingMessage(sourceId, timerAddress.appendSuffix("15000"), check); // check interval

                // Keep reading in msg until keepalive or timeout
                while (true) {
                    cnt.suspend();
                    
                    Object msg = ctx.getIncomingMessage();
                    if (msg == check) { // kill this incominglinksubcoroutine on timeout
                        // stop
                        ctx.addOutgoingMessage(sourceId, logAddress, info("No keepalive from {}, killing incoming link", requesterAddress));
                        return null;
                    }

                    // remove last element of source, as its a temporary identifier used by requestsubcoroutine to identify responses
                    // e.g. actor:1:router:out0:7515937758611767298 -> actor:1:router:out0
                    Address sender = ctx.getSource().removeSuffix(1);
                    
                    if (msg instanceof LinkKeepAliveRequest && sender.equals(requesterAddress)) { // if keepalive from source, queue up
                                                                                                  // response and continue to main loop
                        // state.getLinks() will always be in the form actor:#:router:in# or actor:#:router:out#
                        // we need to strip out the router:in# and router:out#, which means we always need to remove the last 2 elements
                        Set<Address> savedLinks = state.getLinks();
                        Set<Address> correctedLinks = savedLinks.stream().map(x -> x.removeSuffix(2)).collect(Collectors.toSet());
                        
                        ctx.addOutgoingMessage(ctx.getSource(), new LinkKeptAliveResponse(correctedLinks));
                        ctx.addOutgoingMessage(sourceId, logAddress,
                                info("Keepalive arrive from {}, responding with {}", ctx.getSource(), correctedLinks));
                        break;
                    }
                }
            }
        } finally {
            state.removeIncomingLink(requesterAddress);
        }
    }

}
