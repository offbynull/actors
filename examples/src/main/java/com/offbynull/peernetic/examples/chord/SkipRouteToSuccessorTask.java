package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.ExternalSuccessorEntry;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.InternalSuccessorEntry;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.SuccessorEntry;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.coroutines.RequestCoroutine;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.time.Duration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SkipRouteToSuccessorTask implements Coroutine {

    private static final Logger LOG = LoggerFactory.getLogger(SkipRouteToSuccessorTask.class);

    private final ExternalPointer bootstrapNode;
    private final NodeId findId;
    private final NodeId skipId;

    private Pointer found;

    private final String sourceId;
    private final State state;

    public SkipRouteToSuccessorTask(String sourceId, State state, ExternalPointer bootstrapNode, NodeId findId, NodeId skipId) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        Validate.notNull(bootstrapNode);
        Validate.notNull(findId);
        Validate.notNull(skipId);
        this.sourceId = sourceId;
        this.state = state;
        this.bootstrapNode = bootstrapNode;
        this.findId = findId;
        this.skipId = skipId;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        LOG.debug("{} {} - Routing to predecessor of {}", state.getSelfId(), sourceId, findId);
        ExternalPointer pointer = funnelToSkipRouteToCoroutine(cnt, bootstrapNode, findId, skipId);
        if (pointer == null) {
            LOG.debug("{} {} - Failed to route to predecessor of {}", state.getSelfId(), sourceId, findId);
            return;
        }

        LOG.debug("{} {} - Predecessor of {} routed to {}", state.getSelfId(), sourceId, findId, pointer);
        String ptrAddress = ((ExternalPointer) pointer).getAddress();
        GetSuccessorResponse gsr = funnelToRequestCoroutine(
                cnt,
                ptrAddress,
                new GetSuccessorRequest(state.generateExternalMessageId()),
                Duration.ofSeconds(10L),
                GetSuccessorResponse.class);

        SuccessorEntry successorEntry = gsr.getEntries().get(0);

        String succAddress;
        if (successorEntry instanceof InternalSuccessorEntry) { // this means the successor to the node is itself
            succAddress = ptrAddress;
        } else if (successorEntry instanceof ExternalSuccessorEntry) {
            succAddress = ((ExternalSuccessorEntry) successorEntry).getAddress();
        } else {
            throw new IllegalStateException();
        }

        // ask for that successor's id, wait for response here
        GetIdResponse gir = funnelToRequestCoroutine(
                cnt,
                succAddress,
                new GetIdRequest(state.generateExternalMessageId()),
                Duration.ofSeconds(10L),
                GetIdResponse.class);
        found = state.toPointer(gir.getChordId(), succAddress);

        LOG.debug("{} {} - Successor of {} routed to {}", state.getSelfId(), sourceId, findId, found);
    }

    public Pointer getResult() {
        return found;
    }

    private ExternalPointer funnelToSkipRouteToCoroutine(Continuation cnt, ExternalPointer bootstrapNode, NodeId findId, NodeId skipId) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(bootstrapNode);
        Validate.notNull(findId);

        String idSuffix = "initroutetopred" + state.generateExternalMessageId();
        SkipRouteToTask innerCoroutine = new SkipRouteToTask(
                AddressUtils.parentize(sourceId, idSuffix),
                state,
                bootstrapNode,
                findId,
                skipId);
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
}
