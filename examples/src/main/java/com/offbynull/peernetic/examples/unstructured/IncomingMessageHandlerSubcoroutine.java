package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.Controller;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkFailedResponse;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkSuccessResponse;
import com.offbynull.peernetic.examples.unstructured.externalmessages.QueryRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.QueryResponse;
import org.apache.commons.lang3.Validate;

final class IncomingMessageHandlerSubcoroutine implements Subcoroutine<Void> {

    private final Address sourceId;
    private final Address timerAddress;
    private final State state;
    private final Controller controller;

    public IncomingMessageHandlerSubcoroutine(Address sourceId, Address timerAddress, State state, Controller controller) {
        Validate.notNull(sourceId);
        Validate.notNull(timerAddress);
        Validate.notNull(state);
        this.sourceId = sourceId;
        this.timerAddress = timerAddress;
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

        while (true) {
            Object msg = ctx.getIncomingMessage();

            if (msg instanceof QueryRequest) {
                QueryResponse resp = new QueryResponse(state.getLinks());
                ctx.addOutgoingMessage(sourceId, ctx.getSource(), resp);
            } else if (msg instanceof LinkRequest) {
                Address sourceAddress = ctx.getSource();
                
                // if we already have an active incominglinksubcoroutine for the sender, return its id 
                Address updaterId = state.getIncomingLink(sourceAddress);
                if (updaterId != null) {
                    ctx.addOutgoingMessage(sourceId, sourceAddress, new LinkSuccessResponse(updaterId));
                    continue;
                }
                
                // if we don't have any more room for incoming connections, return failure
                if (state.isIncomingLinksFull()) {
                    ctx.addOutgoingMessage(sourceId, sourceAddress, new LinkFailedResponse());
                    continue;
                }
                
                // add the new incominglinksubcoroutine and return success
                updaterId = controller.getSourceId().appendSuffix("in" + state.nextRandomId());
                state.addIncomingLink(sourceAddress, updaterId);
                
                ctx.addOutgoingMessage(sourceId, sourceAddress, new LinkSuccessResponse(updaterId));

                controller.add(new IncomingLinkSubcoroutine(updaterId, timerAddress, state), AddBehaviour.ADD_PRIME);
            }
        }
    }

}
