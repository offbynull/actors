package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.raft.Mode.CANDIDATE;
import static com.offbynull.peernetic.examples.raft.Mode.FOLLOWER;
import com.offbynull.peernetic.examples.raft.externalmessages.AppendEntriesRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.PullEntryRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.PushEntryRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.RedirectResponse;
import com.offbynull.peernetic.examples.raft.externalmessages.RetryResponse;
import com.offbynull.peernetic.examples.raft.internalmessages.ElectionTimeout;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleNode;

final class FollowerSubcoroutine extends AbstractRaftServerSubcoroutine {

    private ElectionTimeout timeoutObj;

    public FollowerSubcoroutine(ServerState state) {
        super(state);
    }

    @Override
    protected Mode main(Continuation cnt, ServerState state) throws Exception {
        Context ctx = (Context) cnt.getContext();

        Address logAddress = state.getLogAddress();
        Address timerAddress = state.getTimerAddress();
        Address graphAddress = state.getGraphAddress();
        String selfLink = state.getSelfLinkId();

        ctx.addOutgoingMessage(logAddress, debug("Entering follower mode"));
        ctx.addOutgoingMessage(graphAddress, new StyleNode(selfLink, 0x0000FF));

        // Set up initial timeout
        timeoutObj = new ElectionTimeout();
        int waitTime = state.nextElectionTimeout();
        ctx.addOutgoingMessage(logAddress, debug("Election timeout waiting for {}ms", waitTime));
        ctx.addOutgoingMessage(timerAddress.appendSuffix("" + waitTime), timeoutObj);

        while (true) {
            cnt.suspend();

            Object msg = ctx.getIncomingMessage();
            if (msg == timeoutObj) {
                // The timeout has been hit without a heartbeat coming in. Switch to candidate mode and exit.
                ctx.addOutgoingMessage(logAddress, debug("Election timeout elapsed, switching to candidate"));

                return CANDIDATE;
            }
        }
    }

    @Override
    protected Mode handleAppendEntriesRequest(Context ctx, AppendEntriesRequest req, ServerState state) throws Exception {
        Mode ret = super.handleAppendEntriesRequest(ctx, req, state);

        // we're already in follower mode, so don't bother returning return FOLLOWER (will force a new follower subcoroutine), but reset the
        // election timeout
        if (ret != FOLLOWER) {
            return ret;
        }

        Address logAddress = state.getLogAddress();
        Address timerAddress = state.getTimerAddress();

        timeoutObj = new ElectionTimeout();
        int waitTime = state.nextElectionTimeout();
        ctx.addOutgoingMessage(logAddress, debug("Got heartbeat. Resetting electing timeout for {}ms", waitTime));
        ctx.addOutgoingMessage(timerAddress.appendSuffix("" + waitTime), timeoutObj);

        return null;
    }

    @Override
    protected Mode handlePushEntryRequest(Context ctx, PushEntryRequest req, ServerState state) throws Exception {
        Address src = ctx.getSource();
        Address logAddress = state.getLogAddress();

        String leaderLinkId = state.getVotedForLinkId();
        if (leaderLinkId == null) {
            ctx.addOutgoingMessage(logAddress, debug("Responding with retry (follower w/o leader)"));
            ctx.addOutgoingMessage(src, new RetryResponse());
        } else {
            ctx.addOutgoingMessage(logAddress, debug("Responding with redirect to {}", leaderLinkId));
            ctx.addOutgoingMessage(src, new RedirectResponse(leaderLinkId));
        }

        return null;
    }

    @Override
    protected Mode handlePullEntryRequest(Context ctx, PullEntryRequest req, ServerState state) throws Exception {
        Address src = ctx.getSource();
        Address logAddress = state.getLogAddress();

        String leaderLinkId = state.getVotedForLinkId();
        if (leaderLinkId == null) {
            ctx.addOutgoingMessage(logAddress, debug("Responding with retry (follower w/o leader)"));
            ctx.addOutgoingMessage(src, new RetryResponse());
        } else {
            ctx.addOutgoingMessage(logAddress, debug("Responding with redirect to {}", leaderLinkId));
            ctx.addOutgoingMessage(src, new RedirectResponse(leaderLinkId));
        }

        return null;
    }
}
