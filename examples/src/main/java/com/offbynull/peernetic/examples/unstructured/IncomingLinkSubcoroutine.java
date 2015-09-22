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
import org.apache.commons.lang3.Validate;

final class IncomingLinkSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    private final Address timerAddress;
    private final Address logAddress;
    private final State state;

    public IncomingLinkSubcoroutine(Address subAddress, State state) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        this.subAddress = subAddress;
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
        this.state = state;
    }

    @Override
    public Address getAddress() {
        return subAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        
        // Validate initial msg is a link request
        Validate.isTrue(ctx.getIncomingMessage() instanceof LinkRequest);
        Address initiatorSourceAddress = ctx.getSource();
        
        // e.g. actor:1:router:out0:7515937758611767298 -> actor:1
        Address initiatorRootSourceAddress = initiatorSourceAddress.removeSuffix(3); 
        // e.g. actor:1 -> 1
        String initiatorLinkId = state.getAddressTransformer().toLinkId(initiatorRootSourceAddress);
        // e.g. actor:1:router:out0:7515937758611767298 -> router:out0
        Address initiatorSuffix = initiatorSourceAddress.removeSuffix(1).removePrefix(initiatorRootSourceAddress);

        
        // In a loop -- wait up to 15 seconds for keepalive. If none arrived (or exception), remove incoming link and leave
        try {
            while (true) {
                ctx.addOutgoingMessage(subAddress, logAddress, info("Waiting for keepalive from {}", initiatorLinkId));
                
                Check check = new Check();
                ctx.addOutgoingMessage(subAddress, timerAddress.appendSuffix("15000"), check); // check interval

                // Keep reading in msg until keepalive or timeout
                while (true) {
                    cnt.suspend();
                    
                    Object msg = ctx.getIncomingMessage();
                    if (msg == check) { // kill this incominglinksubcoroutine on timeout
                        // stop
                        ctx.addOutgoingMessage(subAddress, logAddress, info("No keepalive from {}, killing incoming link", initiatorLinkId));
                        return null;
                    }
                    
                    // if keepalive from source, queue up response and continue to main loop
                    if (msg instanceof LinkKeepAliveRequest) {
                        Address updaterSourceAddress = ctx.getSource();

                        // e.g. actor:1:router:out0:7515937758611767298 -> actor:1
                        Address updaterRootSourceAddress = updaterSourceAddress.removeSuffix(3); 
                        // e.g. actor:1 -> 1
                        String updaterLinkId = state.getAddressTransformer().toLinkId(updaterRootSourceAddress);
                        // e.g. actor:1:router:out0:7515937758611767298 -> router:out0
                        Address updaterSuffix = updaterSourceAddress.removeSuffix(1).removePrefix(updaterRootSourceAddress);
                        
                        if (updaterLinkId.equals(initiatorLinkId) && initiatorSuffix.equals(updaterSuffix)) {
                            ctx.addOutgoingMessage(subAddress, ctx.getSource(), new LinkKeptAliveResponse());
                            ctx.addOutgoingMessage(subAddress, logAddress, info("Keepalive arrive from {}", updaterLinkId));
                            break;
                        }
                    }
                }
            }
        } finally {
            state.removeIncomingLink(initiatorLinkId);
        }
    }

}
