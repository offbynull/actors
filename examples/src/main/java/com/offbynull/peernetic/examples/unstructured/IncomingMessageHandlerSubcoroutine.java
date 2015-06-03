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
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;

final class IncomingMessageHandlerSubcoroutine implements Subcoroutine<Void> {

    private final Address sourceId;
    private final Address timerAddress;
    private final Address logAddress;
    private final State state;
    private final Controller controller;

    public IncomingMessageHandlerSubcoroutine(Address sourceId, Address timerAddress, Address logAddress, State state,
            Controller controller) {
        Validate.notNull(sourceId);
        Validate.notNull(timerAddress);
        Validate.notNull(logAddress);
        Validate.notNull(state);
        this.sourceId = sourceId;
        this.timerAddress = timerAddress;
        this.logAddress = logAddress;
        this.state = state;
        this.controller = controller;
    }

    @Override
    public Address getId() {
        return sourceId;
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
                ctx.addOutgoingMessage(sourceId, logAddress, info("Message from self ({}) ignored: {}", ctx.getSource(), msg));
                continue;
            }
            

            if (msg instanceof QueryRequest) {
                // state.getLinks() will always be in the form actor:#:router:in# or actor:#:router:out#
                // we need to strip out the router:in# and router:out#, which means we always need to remove the last 2 elements
                Set<Address> savedLinks = state.getLinks();
                Set<Address> correctedLinks = savedLinks.stream().map(x -> x.removeSuffix(2)).collect(Collectors.toSet());
                
                QueryResponse resp = new QueryResponse(correctedLinks);
                ctx.addOutgoingMessage(sourceId, ctx.getSource(), resp);
                ctx.addOutgoingMessage(sourceId, logAddress,
                        info("Incoming query request from {}, responding with {}", ctx.getSource(), correctedLinks));
            } else if (msg instanceof LinkRequest) {
                ctx.addOutgoingMessage(sourceId, logAddress, info("Incoming link request from {}", ctx.getSource()));

                // remove suffix of sourceAddress
                Address mainSourceAddress = ctx.getSource().removeSuffix(3);    // e.g. actor:1:router:out0:1234 -> actor:1
                Address outSlotSourceAddress = ctx.getSource().removeSuffix(1); // e.g. actor:1:router:out0:1234 -> actor:1:router:out0

                // if we already have an active incominglinksubcoroutine for the sender, return its id 
                Address keepAliveId = state.getIncomingLink(outSlotSourceAddress);
                if (keepAliveId != null) {
                    ctx.addOutgoingMessage(sourceId, logAddress, info("Already have an incoming link with id {}", keepAliveId));
                    ctx.addOutgoingMessage(sourceId, ctx.getSource(), new LinkSuccessResponse(keepAliveId));
                    continue;
                }

                // if we already have a link in place for the sender, return a failure
                for (Address link : state.getLinks()) {
                    if (mainSourceAddress.isPrefixOf(link)) {
                        ctx.addOutgoingMessage(sourceId, logAddress,
                                warn("Rejecting link from {} (already linked), trying again", mainSourceAddress));
                        ctx.addOutgoingMessage(sourceId, ctx.getSource(), new LinkFailedResponse());
                        continue top;
                    }
                }
            
                // if we don't have any more room for incoming connections, return failure
                if (state.isIncomingLinksFull()) {
                    ctx.addOutgoingMessage(sourceId, logAddress, info("No free incoming link slots available"));
                    ctx.addOutgoingMessage(sourceId, ctx.getSource(), new LinkFailedResponse());
                    continue;
                }

                // add the new incominglinksubcoroutine and return success
                keepAliveId = controller.getSourceId().appendSuffix("in" + keepAliveCounter);
                state.addIncomingLink(outSlotSourceAddress, keepAliveId);

                keepAliveCounter++;

                ctx.addOutgoingMessage(sourceId, logAddress, info("Added incoming link slot with id {}", keepAliveId));
                ctx.addOutgoingMessage(sourceId, ctx.getSource(), new LinkSuccessResponse(keepAliveId));

                controller.add(new IncomingLinkSubcoroutine(keepAliveId, timerAddress, logAddress, state), AddBehaviour.ADD_PRIME_NO_FINISH);
            }
        }
    }

}
