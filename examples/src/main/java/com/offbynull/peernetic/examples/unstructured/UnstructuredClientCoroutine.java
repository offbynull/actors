package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.examples.unstructured.internalmessages.Start;
import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.visualizer.gateways.graph.AddNode;
import com.offbynull.peernetic.visualizer.gateways.graph.MoveNode;
import java.util.Random;

public final class UnstructuredClientCoroutine implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        Start start = ctx.getIncomingMessage();
        Address timerAddress = start.getTimerPrefix();
        Address graphAddress = start.getGraphAddress();
        long seed = start.getSeed();

        Random random = new Random(seed);

        State state = new State(seed, 3, 4);

        ctx.addOutgoingMessage(graphAddress, new AddNode(ctx.getSelf().toString()));
        ctx.addOutgoingMessage(graphAddress,
                new MoveNode(ctx.getSelf().toString(),
                        random.nextInt(1400),
                        random.nextInt(1400))
        );

        SubcoroutineRouter outgoingLinkRouter = new SubcoroutineRouter(Address.of("router"), ctx);
        outgoingLinkRouter.getController().add(
                new IncomingMessageHandlerSubcoroutine(
                        Address.of("handler"),
                        timerAddress,
                        state,
                        outgoingLinkRouter.getController()),
                AddBehaviour.ADD_PRIME_NO_FINISH);
        outgoingLinkRouter.getController().add(
                new OutgoingQuerySubcoroutine(
                        Address.of("querier"),
                        timerAddress,
                        addressCache),
                AddBehaviour.ADD_PRIME_NO_FINISH);
        ADD OUTGOING LINK SUBCOURTINES HERE;

        while (true) {
            cnt.suspend();
            outgoingLinkRouter.forward();
        }
    }
}
