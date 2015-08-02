package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine.IndividualResponseAction;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.raft.Mode.FOLLOWER;
import static com.offbynull.peernetic.examples.raft.Mode.LEADER;
import com.offbynull.peernetic.examples.raft.externalmessages.PullEntryRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.PushEntryRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.RequestVoteRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.RequestVoteResponse;
import com.offbynull.peernetic.examples.raft.externalmessages.RetryResponse;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleNode;
import java.time.Duration;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.mutable.MutableInt;

final class CandidateSubcoroutine extends AbstractRaftServerSubcoroutine {

    public CandidateSubcoroutine(ServerState state) {
        super(state);
    }

    @Override
    protected Mode main(Continuation cnt, ServerState state) throws Exception {
        Context ctx = (Context) cnt.getContext();

        Address logAddress = state.getLogAddress();
        Address timerAddress = state.getTimerAddress();
        Address graphAddress = state.getGraphAddress();
        String selfLink = state.getSelfLinkId();
        IdGenerator idGenerator = state.getIdGenerator();

        ctx.addOutgoingMessage(logAddress, debug("Entering candidate mode"));
        ctx.addOutgoingMessage(graphAddress, new StyleNode(selfLink, "-fx-background-color: yellow"));

        while (true) {
            ctx.addOutgoingMessage(logAddress, debug("Starting new election"));

            int currentTerm = state.incrementCurrentTerm();

            UnmodifiableSet<String> otherLinkIds = state.getOtherNodeLinkIds();

            int requiredSuccessfulCount = state.getMajorityCount();
            MutableInt successfulCount = new MutableInt(1); // start with 1 because we're voting for ourself first

            int lastLogIndex = state.getLastLogIndex();
            int lastLogTerm = state.getLastLogEntry().getTerm();

            Object req = new RequestVoteRequest(currentTerm, lastLogIndex, lastLogTerm);
            int totalWaitTime = state.nextElectionTimeout();
            int attempts = 5;
            int waitTimePerAttempt = totalWaitTime / attempts; // divide by n attempts
            MultiRequestSubcoroutine.Builder<RequestVoteResponse> builder = new MultiRequestSubcoroutine.Builder<RequestVoteResponse>()
                    .timerAddress(timerAddress)
                    .attemptInterval(Duration.ofMillis(waitTimePerAttempt))
                    .maxAttempts(5)
                    .request(req)
                    .addExpectedResponseType(RequestVoteResponse.class)
                    .individualResponseListener(x -> {
                        // This IndividualResponseListener will stop the MultiRequestSubcoroutine once more than half of the responses come
                        // back as "successful"
                        RequestVoteResponse response = x.getResponse();
                        if (response.isVoteGranted()) {
                            successfulCount.increment();
                        }

                        if (successfulCount.getValue() >= requiredSuccessfulCount) {
                            return new IndividualResponseAction(true, true);
                        } else {
                            return new IndividualResponseAction(true, false);
                        }
                    })
                    .sourceAddress(Address.of(idGenerator.generate()));

            AddressTransformer addressTransformer = state.getAddressTransformer();
            for (String linkId : otherLinkIds) {
                Address dstAddr = addressTransformer.linkIdToRemoteAddress(linkId);
                builder.addDestinationAddress(idGenerator, dstAddr);
            }

            MultiRequestSubcoroutine<RequestVoteResponse> multiReq = builder.build();
            multiReq.run(cnt);

            if (successfulCount.getValue() >= requiredSuccessfulCount) {
                // Majority of votes have come in for this node. Set mode to leader.
                ctx.addOutgoingMessage(logAddress, debug("Enough votes acquired, becoming leader... Got: {} Required: {}",
                        successfulCount.getValue(), requiredSuccessfulCount));
                return LEADER;
            } else {
                ctx.addOutgoingMessage(logAddress, debug("Not enough votes, becoming follower... Got: {} Required: {}",
                        successfulCount.getValue(), requiredSuccessfulCount));
                return FOLLOWER;
            }
        }
    }

    @Override
    protected Mode handlePushEntryRequest(Context ctx, PushEntryRequest req, ServerState state) throws Exception {
        Address src = ctx.getSource();
        Address logAddress = state.getLogAddress();

        ctx.addOutgoingMessage(logAddress, debug("Responding with retry (candidate)"));
        ctx.addOutgoingMessage(src, new RetryResponse());

        return null;
    }

    @Override
    protected Mode handlePullEntryRequest(Context ctx, PullEntryRequest req, ServerState state) throws Exception {
        Address src = ctx.getSource();
        Address logAddress = state.getLogAddress();

        ctx.addOutgoingMessage(logAddress, debug("Responding with retry (candidate)"));
        ctx.addOutgoingMessage(src, new RetryResponse());
        
        return null;
    }
}
