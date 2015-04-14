package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.externalmessages.FindSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.FindSuccessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.ExternalSuccessorEntry;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.InternalSuccessorEntry;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.SuccessorEntry;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.coroutines.RequestCoroutine;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.time.Duration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RemoteRouteToTask implements Coroutine {
    
    private static final Logger LOG = LoggerFactory.getLogger(RemoteRouteToTask.class);

    private final NodeId findId;
    private final FindSuccessorRequest originalRequest;
    private final String originalSource;
    
    private final String sourceId;
    private final State state;

    public RemoteRouteToTask(String sourceId, State state, NodeId findId, FindSuccessorRequest originalRequest,
            String originalSource) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        Validate.notNull(findId);
        Validate.notNull(originalRequest);
        Validate.notNull(originalSource);
        this.sourceId = sourceId;
        this.state = state;
        this.findId = findId;
        this.originalRequest = originalRequest;
        this.originalSource = originalSource;
    }
    
    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        Pointer foundSucc;
        try {
            foundSucc = funnelToRouteToCoroutine(cnt, findId);
        } catch (RuntimeException coe) {
            LOG.warn("Unable to route to node");
            return;
        }

        NodeId foundId;
        String foundAddress = null;

        boolean isInternalPointer = foundSucc instanceof InternalPointer;
        boolean isExternalPointer = foundSucc instanceof ExternalPointer;

        if (isInternalPointer) {
            Pointer successor = state.getSuccessor();

            if (successor instanceof InternalPointer) {
                foundId = successor.getId(); // id will always be the same as us
            } else if (successor instanceof ExternalPointer) {
                ExternalPointer externalSuccessor = (ExternalPointer) successor;
                foundId = externalSuccessor.getId();
                foundAddress = externalSuccessor.getAddress();
            } else {
                throw new IllegalStateException();
            }
        } else if (isExternalPointer) {
            try {
                ExternalPointer externalPred = (ExternalPointer) foundSucc;
                GetSuccessorResponse gsr = funnelToRequestCoroutine(
                        cnt,
                        externalPred.getAddress(),
                        new GetSuccessorRequest(state.generateExternalMessageId()),
                        Duration.ofSeconds(10L),
                        GetSuccessorResponse.class);
                SuccessorEntry successorEntry = gsr.getEntries().get(0);

                String senderAddress = ctx.getSource();
                String address;
                if (successorEntry instanceof InternalSuccessorEntry) { // this means the successor to the node is itself
                    address = senderAddress;
                } else if (successorEntry instanceof ExternalSuccessorEntry) {
                    address = ((ExternalSuccessorEntry) successorEntry).getAddress();
                } else {
                    throw new IllegalStateException();
                }

                // ask for that successor's id, wait for response here
                GetIdResponse gir = funnelToRequestCoroutine(
                        cnt,
                        address,
                        new GetIdRequest(state.generateExternalMessageId()),
                        Duration.ofSeconds(10L),
                        GetIdResponse.class);
                foundId = state.toId(gir.getChordId());
                foundAddress = ctx.getSource();
            } catch (RuntimeException coe) {
                LOG.warn("Unable to get successor of node routed to.");
                return;
            }
        } else {
            throw new IllegalStateException();
        }

        addOutgoingExternalMessage(ctx, originalSource,
                new FindSuccessorResponse(originalRequest.getId(), foundId.getValueAsByteArray(), foundAddress));
    }
    
    private Pointer funnelToRouteToCoroutine(Continuation cnt, NodeId findId) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(findId);
        
        String idSuffix = "" + state.generateExternalMessageId();
        RouteToTask innerCoroutine = new RouteToTask(
                AddressUtils.parentize(sourceId, idSuffix),
                state,
                findId);
        innerCoroutine.run(cnt);
        return innerCoroutine.getResult();
    }
    
    private <T extends ExternalMessage> T funnelToRequestCoroutine(Continuation cnt, String destination, ExternalMessage message,
            Duration timeoutDuration, Class<T> expectedResponseClass) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(destination);
        Validate.notNull(message);
        Validate.notNull(timeoutDuration);
        Validate.isTrue(!timeoutDuration.isNegative());
        
        RequestCoroutine requestCoroutine = new RequestCoroutine(
                AddressUtils.parentize(sourceId, "" + message.getId()),
                destination,
                message,
                state.getTimerPrefix(),
                timeoutDuration,
                expectedResponseClass);
        requestCoroutine.run(cnt);
        return requestCoroutine.getResponse();
    }
    
    private void addOutgoingExternalMessage(Context ctx, String destination, ExternalMessage message) {
        Validate.notNull(ctx);
        Validate.notNull(destination);
        Validate.notNull(message);
        
        ctx.addOutgoingMessage(
                AddressUtils.parentize(sourceId, "" + message.getId()),
                destination,
                message);
    }
}
