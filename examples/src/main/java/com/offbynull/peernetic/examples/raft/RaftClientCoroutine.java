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
import com.offbynull.peernetic.examples.raft.externalmessages.RedirectResponse;
import com.offbynull.peernetic.examples.raft.externalmessages.PushEntryRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.RetryResponse;
import com.offbynull.peernetic.examples.raft.externalmessages.PushEntryResponse;
import com.offbynull.peernetic.examples.raft.internalmessages.StartClient;
import com.offbynull.peernetic.visualizer.gateways.graph.AddEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.AddNode;
import com.offbynull.peernetic.visualizer.gateways.graph.LabelNode;
import com.offbynull.peernetic.visualizer.gateways.graph.MoveNode;
import com.offbynull.peernetic.visualizer.gateways.graph.RemoveEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.RemoveNode;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleNode;
import java.time.Duration;
import java.util.LinkedList;

public final class RaftClientCoroutine implements Coroutine {
    
    private static final String NODE_LABEL_FORMAT = "%s\n"
            + "\n"
            + "readval:%s\n"
            + "readidx:%d\n"
            + "readterm:%d\n"
            + "\n"
            + "writeval:%s\n"
            + "writeidx:%d\n"
            + "writeterm:%d";

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        StartClient start = ctx.getIncomingMessage();
        Address timerAddress = start.getTimerAddress();
        Address graphAddress = start.getGraphAddress();
        Address logAddress = start.getLogAddress();
        int maxAttempts = 5;
        int waitTimePerRequestAttempt = (start.getMaxElectionTimeout() * 2) / maxAttempts;
        LinkedList<String> serverLinks = new LinkedList<>(start.getNodeLinks());
        AddressTransformer addressTransformer = start.getAddressTransformer();
        
        Address self = ctx.getSelf();
        String selfLink = addressTransformer.selfAddressToLinkId(self);

        String leaderLinkId = rotateToNextServer(serverLinks);
        
        ctx.addOutgoingMessage(logAddress, debug("Starting client"));
        ctx.addOutgoingMessage(graphAddress, new AddNode(selfLink));
        ctx.addOutgoingMessage(graphAddress, new MoveNode(selfLink, 0.0, 0.0));
        ctx.addOutgoingMessage(graphAddress, new StyleNode(selfLink, "-fx-background-color: gray"));
        
        try {
            int nextWriteValue = 1000;
            
            Object lastWriteValue = null;
            int lastWriteIndex = -1;
            int lastWriteTerm = -1;
            Object lastReadValue = null;
            int lastReadIndex = -1;
            int lastReadTerm = -1;
            
            while (true) {
                ctx.addOutgoingMessage(logAddress, debug("Waiting 1 second"));
                
                Object timerObj = new Object();
                ctx.addOutgoingMessage(timerAddress.appendSuffix("1000"), timerObj);
                while (true) {
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
                        .sourceAddress(Address.of())
                        .request(pushReq)
                        .timerAddress(timerAddress)
                        .destinationAddress(dstAddress)
                        .maxAttempts(maxAttempts)
                        .attemptInterval(Duration.ofMillis(waitTimePerRequestAttempt))
                        .addExpectedResponseType(PushEntryResponse.class)
                        .addExpectedResponseType(RetryResponse.class)
                        .addExpectedResponseType(RedirectResponse.class)
                        .throwExceptionIfNoResponse(false)
                        .build();
                Object pushResp = pushRequestSubcoroutine.run(cnt);

                if (pushResp == null) {
                    leaderLinkId = rotateToNextServer(serverLinks);
                    ctx.addOutgoingMessage(logAddress, debug("Failed to push log entry {}, no response", writeValue));
                    ctx.addOutgoingMessage(logAddress, debug("Leader changed {}", leaderLinkId));
                    continue;                
                } else if (pushResp instanceof RetryResponse) {
                    ctx.addOutgoingMessage(logAddress, debug("Failed to push log entry {}, bad state", writeValue));
                    continue;
                } else if (pushResp instanceof RedirectResponse) {
                    String newLeaderLinkId = ((RedirectResponse) pushResp).getLeaderLinkId();
                    ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfLink, leaderLinkId));
                    ctx.addOutgoingMessage(graphAddress, new AddEdge(selfLink, newLeaderLinkId));
                    leaderLinkId = newLeaderLinkId;
                    ctx.addOutgoingMessage(logAddress, debug("Failed to push log entry {}, leader changed {}", writeValue, leaderLinkId));
                    continue;
                } else if (pushResp instanceof PushEntryResponse) {
                    PushEntryResponse msg = (PushEntryResponse) pushResp;
                    lastWriteValue = writeValue;
                    lastWriteIndex = msg.getIndex();
                    lastWriteTerm = msg.getTerm();
                    ctx.addOutgoingMessage(graphAddress, new LabelNode(selfLink, String.format(NODE_LABEL_FORMAT,
                            selfLink,
                            lastReadValue,
                            lastReadIndex,
                            lastReadTerm,
                            lastWriteValue,
                            lastWriteIndex,
                            lastWriteTerm)));
                    ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfLink, leaderLinkId));
                    ctx.addOutgoingMessage(graphAddress, new AddEdge(selfLink, leaderLinkId));
                    ctx.addOutgoingMessage(logAddress, debug("Successfully pushed log entry {}", writeValue));
                } else {
                    throw new IllegalStateException();
                }


                
                
                ctx.addOutgoingMessage(logAddress, debug("Attempting to pull log entry from {}", leaderLinkId));
                PullEntryRequest pullReq = new PullEntryRequest();
                RequestSubcoroutine<Object> pullRequestSubcoroutine = new RequestSubcoroutine.Builder<>()
                        .sourceAddress(Address.of())
                        .request(pullReq)
                        .timerAddress(timerAddress)
                        .destinationAddress(dstAddress)
                        .maxAttempts(maxAttempts)
                        .attemptInterval(Duration.ofMillis(waitTimePerRequestAttempt))
                        .addExpectedResponseType(RetryResponse.class)
                        .addExpectedResponseType(RedirectResponse.class)
                        .addExpectedResponseType(PullEntryResponse.class)
                        .throwExceptionIfNoResponse(false)
                        .build();
                Object pullResp = pullRequestSubcoroutine.run(cnt);

                if (pullResp == null) {
                    leaderLinkId = rotateToNextServer(serverLinks);
                    ctx.addOutgoingMessage(logAddress, debug("Failed to pull log entry, no response"));
                    ctx.addOutgoingMessage(logAddress, debug("Leader changed {}", leaderLinkId));
                    continue;
                } else if (pullResp instanceof RetryResponse) {
                    ctx.addOutgoingMessage(logAddress, debug("Failed to pull log entry {}, bad state", writeValue));
                    continue;
                } else if (pullResp instanceof RedirectResponse) {
                    String newLeaderLinkId = ((RedirectResponse) pullResp).getLeaderLinkId();
                    ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfLink, leaderLinkId));
                    ctx.addOutgoingMessage(graphAddress, new AddEdge(selfLink, newLeaderLinkId));
                    leaderLinkId = newLeaderLinkId;
                    ctx.addOutgoingMessage(logAddress, debug("Failed to pull log entry {}, leader changed {}", writeValue, leaderLinkId));
                    continue;
                } else {
                    PullEntryResponse msg = (PullEntryResponse) pullResp;
                    lastReadValue = msg.getValue();
                    lastReadIndex = msg.getIndex();
                    lastReadTerm = msg.getTerm();
                    ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfLink, leaderLinkId));
                    ctx.addOutgoingMessage(graphAddress, new AddEdge(selfLink, leaderLinkId));
                    ctx.addOutgoingMessage(graphAddress, new LabelNode(selfLink, String.format(NODE_LABEL_FORMAT,
                            selfLink,
                            lastReadValue,
                            lastReadIndex,
                            lastReadTerm,
                            lastWriteValue,
                            lastWriteIndex,
                            lastWriteTerm)));
                    ctx.addOutgoingMessage(logAddress, debug("Successfully pulled log entry {} at index {}", lastReadValue, lastReadIndex));
                }
            }
        } finally {
            ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfLink, leaderLinkId));
            ctx.addOutgoingMessage(graphAddress, new RemoveNode(selfLink));
        }
    }
    
    private String rotateToNextServer(LinkedList<String> serverLinks) {
        String ret = serverLinks.removeFirst();
        serverLinks.addLast(ret);
        
        return ret;
    }
}
