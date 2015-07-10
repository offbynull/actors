package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.Controller;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.info;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.warn;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkFailedResponse;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkSuccessResponse;
import com.offbynull.peernetic.examples.unstructured.externalmessages.QueryRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.QueryResponse;
import java.util.Set;
import org.apache.commons.lang3.Validate;

final class IncomingMessageHandlerSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    private final Address timerAddress;
    private final Address logAddress;
    private final State state;
    private final Controller controller;

    public IncomingMessageHandlerSubcoroutine(Address subAddress, Address timerAddress, Address logAddress, State state,
            Controller controller) {
        Validate.notNull(subAddress);
        Validate.notNull(timerAddress);
        Validate.notNull(logAddress);
        Validate.notNull(state);
        this.subAddress = subAddress;
        this.timerAddress = timerAddress;
        this.logAddress = logAddress;
        this.state = state;
        this.controller = controller;
    }

    @Override
    public Address getAddress() {
        return subAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        int keepAliveCounter = 0;
        top:
        while (true) {
            cnt.suspend();
            
            Object msg = ctx.getIncomingMessage();
            
            if (ctx.getSelf().isPrefixOf(ctx.getSource())) {
                ctx.addOutgoingMessage(subAddress, logAddress, info("Message from self ({}) ignored: {}", ctx.getSource(), msg));
                continue;
            }
            

            if (msg instanceof QueryRequest) {
                Set<String> linkIds = state.getLinks();
                
                QueryResponse resp = new QueryResponse(linkIds);
                ctx.addOutgoingMessage(subAddress, ctx.getSource(), resp);
                ctx.addOutgoingMessage(subAddress, logAddress,
                        info("Incoming query request from {}, responding with {}", ctx.getSource(), linkIds));
            } else if (msg instanceof LinkRequest) {
                ctx.addOutgoingMessage(subAddress, logAddress, info("Incoming link request from {}", ctx.getSource()));

                // remove suffix of sourceAddress
                Address reqSourceAddress = ctx.getSource().removeSuffix(3);    // e.g. actor:1:router:out0:1234 -> actor:1
                String reqLinkId = state.getAddressTransformer().remoteAddressToLinkId(reqSourceAddress);

                // if we already have an active incominglinksubcoroutine for the sender, return its id 
                Address reqSuffix = state.getIncomingLinkSuffix(reqLinkId);
                if (reqSuffix != null) {
                    ctx.addOutgoingMessage(subAddress, logAddress, info("Already have an incoming link with id {}", reqSuffix));
                    ctx.addOutgoingMessage(subAddress, ctx.getSource(), new LinkSuccessResponse(reqSuffix));
                    continue;
                }

                // if we already have a link in place for the sender, return a failure
                if (state.getLinks().contains(reqLinkId)) {
                    ctx.addOutgoingMessage(subAddress, logAddress,
                            warn("Rejecting link from {} (already linked), trying again", reqSourceAddress));
                    ctx.addOutgoingMessage(subAddress, ctx.getSource(), new LinkFailedResponse());
                    continue top;
                }
            
                // if we don't have any more room for incoming connections, return failure
                if (state.isIncomingLinksFull()) {
                    ctx.addOutgoingMessage(subAddress, logAddress, info("No free incoming link slots available"));
                    ctx.addOutgoingMessage(subAddress, ctx.getSource(), new LinkFailedResponse());
                    continue;
                }

                // add the new incominglinksubcoroutine and return success
                reqSuffix = controller.getSourceId().appendSuffix("in" + keepAliveCounter);
                state.addIncomingLink(reqLinkId, reqSuffix);

                keepAliveCounter++;

                ctx.addOutgoingMessage(subAddress, logAddress, info("Added incoming link slot with id {}", reqSuffix));
                ctx.addOutgoingMessage(subAddress, ctx.getSource(), new LinkSuccessResponse(reqSuffix));

                controller.add(new IncomingLinkSubcoroutine(reqSuffix, timerAddress, logAddress, state), AddBehaviour.ADD_PRIME_NO_FINISH);
            }
        }
    }

}
