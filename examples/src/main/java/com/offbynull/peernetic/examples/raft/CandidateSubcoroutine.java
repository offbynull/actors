package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine.IndividualResponseAction;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine.Response;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD_PRIME_NO_FINISH;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.Controller;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.raft.AddressConstants.ROUTER_CANDIDATE_RELATIVE_ADDRESS;
import com.offbynull.peernetic.examples.raft.externalmessages.RequestVoteRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.RequestVoteResponse;
import java.time.Duration;
import java.util.List;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;

final class CandidateSubcoroutine implements Subcoroutine<Void> {

    private static final Address SUB_ADDRESS = ROUTER_CANDIDATE_RELATIVE_ADDRESS;

    private final State state;
    
    private final Address timerAddress;
    private final Address logAddress;
    private final Controller controller;
    
    public CandidateSubcoroutine(State state) {
        Validate.notNull(state);

        this.state = state;
        
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
        this.controller = state.getRouterController();
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        while (true) {
            int newTerm = state.incrementCurrentTerm();

            UnmodifiableSet<String> otherLinkIds = state.getOtherNodeLinkIds();
            
            int totalCount = otherLinkIds.size() + 1; // + 1 because we're including ourself in the count
            int requiredSuccessfulCount = (totalCount / 2) + 1; // more than half, e.g. if 6 then (6/2)+1=4 ... e.g. if 7 then (7/2)+1=4
            MutableInt successfulCount = new MutableInt(1); // start with 1 because we're voting for ourself first
            
            String multiReqId = state.nextRandomId();
            MultiRequestSubcoroutine.Builder<RequestVoteResponse> builder = new MultiRequestSubcoroutine.Builder<RequestVoteResponse>()
                    .timerAddressPrefix(timerAddress)
                    .attemptInterval(Duration.ofMillis(200L))
                    .maxAttempts(5)
                    .request(new RequestVoteRequest(newTerm))
                    .addExpectedResponseType(RequestVoteResponse.class)
                    .individualResponseListener(x -> {
                        // This IndividualResponseListener will stop the MultiRequestSubcoroutine once more than half of the responses come
                        // back as "successful"
                        RequestVoteResponse response = x.getResponse();
                        if (response.isSuccess()) {
                            successfulCount.increment();
                        }
                        
                        if (successfulCount.getValue() >= requiredSuccessfulCount) {
                            return new IndividualResponseAction(true, true);
                        } else {
                            return new IndividualResponseAction(true, false);
                        }
                    })
                    .address(SUB_ADDRESS.appendSuffix(multiReqId));

            AddressTransformer addressTransformer = state.getAddressTransformer();
            for (String linkId : otherLinkIds) {
                String msgId = state.nextRandomId();
                Address dstAddr = addressTransformer.linkIdToRemoteAddress(linkId);
                builder.addDestination(msgId, dstAddr);
            }

            MultiRequestSubcoroutine<RequestVoteResponse> multiReq = builder.build();
            multiReq.run(cnt);

            if (successfulCount.getValue() >= requiredSuccessfulCount) {
                // Majority of votes have come in for this node.
                ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Elected as leader successfully on term {}", newTerm));
                controller.add(new CandidateSubcoroutine(state), ADD_PRIME_NO_FINISH);
                return null;
            }
        }
    }
    
    @Override
    public Address getAddress() {
        return SUB_ADDRESS;
    }
}
