package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD_PRIME_NO_FINISH;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.Controller;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.raft.AddressConstants.ROUTER_HANDLER_RELATIVE_ADDRESS;
import com.offbynull.peernetic.examples.raft.internalmessages.Kill;
import com.offbynull.peernetic.examples.raft.internalmessages.Start;
import org.apache.commons.collections4.set.UnmodifiableSet;

public final class RaftServerCoroutine implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        Start start = ctx.getIncomingMessage();
        Address timerAddress = start.getTimerPrefix();
        Address graphAddress = start.getGraphAddress();
        Address logAddress = start.getLogAddress();
        UnmodifiableSet<String> nodeLinks = start.getNodeLinks();
        AddressTransformer addressTransformer = start.getAddressTransformer();
        long seed = start.getSeed();

        Address self = ctx.getSelf();
        String selfLink = addressTransformer.selfAddressToLinkId(self);
        
        SubcoroutineRouter router = new SubcoroutineRouter(ROUTER_HANDLER_RELATIVE_ADDRESS, ctx);
        Controller routerController = router.getController();
        
        State state = new State(timerAddress, graphAddress, logAddress, seed, selfLink, nodeLinks, addressTransformer, routerController);

        routerController.add(new IncomingRequestHandlerSubcoroutine(state), ADD);
        routerController.add(new FollowerSubcoroutine(state), ADD_PRIME_NO_FINISH);
        
        // Process messages
        while (true) {
            cnt.suspend();

            // if sent to main address then forward to incoming request handler, otherwise forward to router
            boolean forwardedToRouter = router.forward().isForwarded();
            if (!forwardedToRouter) {
                Object msg = ctx.getIncomingMessage();
                boolean isFromSelf = ctx.getSource().equals(ctx.getSelf());
                if (isFromSelf && msg instanceof Kill) {
                    throw new RuntimeException("Kill message encountered");
                }
            }
        }
    }
}
