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

final class IncomingHandlerSubcoroutine implements Subcoroutine<Void> {

    private final Address sourceId;
    private final Address timerAddress;
    private final State state;
    private final Controller controller;

    public IncomingHandlerSubcoroutine(Address sourceId, Address timerAddress, State state, Controller controller) {
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
                if (state.isIncomingLinksFull()) {
                    ctx.addOutgoingMessage(sourceId, ctx.getSource(), new LinkFailedResponse());
                    continue;
                }
                
                state.addIncomingLink(ctx.getSource());
                Address newId = controller.getSourceId().appendSuffix("" + state.nextRandomId());
                ctx.addOutgoingMessage(sourceId, ctx.getSource(), new LinkSuccessResponse(newId));

                controller.add(new IncomingLinkSubcoroutine(newId, timerAddress, state), AddBehaviour.ADD_PRIME);
            }
        }
    }

}
