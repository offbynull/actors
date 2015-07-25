package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.raft.externalmessages.PullEntryRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.PullEntryResponse;
import com.offbynull.peernetic.examples.raft.externalmessages.PushEntryRedirectResponse;
import com.offbynull.peernetic.examples.raft.externalmessages.PushEntryRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.PushEntryRetryResponse;
import com.offbynull.peernetic.examples.raft.externalmessages.PushEntrySuccessResponse;
import com.offbynull.peernetic.examples.raft.internalmessages.StartClient;
import org.apache.commons.collections4.set.UnmodifiableSet;

public final class RaftClientCoroutine implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        StartClient start = ctx.getIncomingMessage();
        Address timerAddress = start.getTimerPrefix();
        Address graphAddress = start.getGraphAddress();
        Address logAddress = start.getLogAddress();
        UnmodifiableSet<String> nodeLinks = start.getNodeLinks();
        AddressTransformer addressTransformer = start.getAddressTransformer();

        ctx.addOutgoingMessage(logAddress, debug("Starting client"));
        
        String leaderLinkId = nodeLinks.iterator().next();
        int nextWriteValue = 1000;
        while (true) {
            ctx.addOutgoingMessage(logAddress, debug("Waiting 1 second"));
            while (true) {
                Object timerObj = new Object();
                ctx.addOutgoingMessage(timerAddress.appendSuffix("1000"), timerObj);
                cnt.suspend();

                if (ctx.getIncomingMessage() == timerObj) {
                    break;
                }
            }
            
            
            Address dstAddress = addressTransformer.linkIdToRemoteAddress(leaderLinkId);
            
            int writeValue = nextWriteValue;
            nextWriteValue++;
            ctx.addOutgoingMessage(logAddress, debug("Attempting to push log entry {} in to {}", writeValue, leaderLinkId));
            PushEntryRequest pushReq = new PushEntryRequest(writeValue);
            RequestSubcoroutine<Object> pushRequestSubcoroutine = new RequestSubcoroutine.Builder<>()
                    .request(pushReq)
                    .timerAddressPrefix(timerAddress)
                    .destinationAddress(dstAddress)
                    .addExpectedResponseType(PushEntrySuccessResponse.class)
                    .addExpectedResponseType(PushEntryRetryResponse.class)
                    .addExpectedResponseType(PushEntryRedirectResponse.class)
                    .throwExceptionIfNoResponse(false)
                    .build();
            Object pushResp = pushRequestSubcoroutine.run(cnt);
            
            if (pushResp == null) {
                ctx.addOutgoingMessage(logAddress, debug("Failed to push log entry {}, no response", writeValue));
                continue;                
            } else if (pushResp instanceof PushEntryRetryResponse) {
                ctx.addOutgoingMessage(logAddress, debug("Failed to push log entry {}, bad state", writeValue));
                continue;
            } else if (pushResp instanceof PushEntryRedirectResponse) {
                leaderLinkId = ((PushEntryRedirectResponse) pushResp).getLeaderLinkId();
                ctx.addOutgoingMessage(logAddress, debug("Failed to push log entry {}, leader changed {}", writeValue, leaderLinkId));
                continue;
            } else if (pushResp instanceof PushEntrySuccessResponse) {
                ctx.addOutgoingMessage(logAddress, debug("Successfully pushed log entry {}", writeValue));
            } else {
                throw new IllegalStateException();
            }
            
            
            ctx.addOutgoingMessage(logAddress, debug("Attempting to pull log entry from {}", leaderLinkId));
            PullEntryRequest pullReq = new PullEntryRequest();
            RequestSubcoroutine<Object> pullRequestSubcoroutine = new RequestSubcoroutine.Builder<>()
                    .request(pullReq)
                    .timerAddressPrefix(timerAddress)
                    .destinationAddress(dstAddress)
                    .addExpectedResponseType(PullEntryResponse.class)
                    .throwExceptionIfNoResponse(false)
                    .build();
            Object pullResp = pullRequestSubcoroutine.run(cnt);
            
            if (pullResp == null) {
                ctx.addOutgoingMessage(logAddress, debug("Failed to pull log entry, no response"));
                continue;
            } else {
                PullEntryResponse msg = (PullEntryResponse) pullResp;
                Object readValue = msg.getValue();
                int idx = msg.getIndex();
                ctx.addOutgoingMessage(logAddress, debug("Successfully pulled log entry {} at index {}", readValue, idx));
            }
        }
    }
}
