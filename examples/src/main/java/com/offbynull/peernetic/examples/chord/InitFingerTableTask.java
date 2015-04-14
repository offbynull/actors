package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.externalmessages.FindSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.FindSuccessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetPredecessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetPredecessorResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.FingerTable;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.SuccessorTable;
import com.offbynull.peernetic.examples.common.coroutines.RequestCoroutine;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.time.Duration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InitFingerTableTask implements Coroutine {
    
    private static final Logger LOG = LoggerFactory.getLogger(InitFingerTableTask.class);

    private final ExternalPointer bootstrapNode;
    private final String sourceId;
    private final State state;

    public InitFingerTableTask(String sourceId, State state, ExternalPointer bootstrapNode) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        Validate.notNull(bootstrapNode);
        this.sourceId = sourceId;
        this.state = state;
        this.bootstrapNode = bootstrapNode;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        int len = state.getFingerTableLength();

        // NOTE: everything up until the populate finger loop is an essential operation... if a failure occurs, joining has to fail
        
        // create fingertable -- find our successor from the bootstrap node and store it in the fingertable
        FingerTable fingerTable = new FingerTable(state.getSelfPointer());
        NodeId expectedSuccesorId = fingerTable.getExpectedId(0);

        FindSuccessorResponse fsr = funnelToRequestCoroutine(
                cnt,
                bootstrapNode.getAddress(),
                new FindSuccessorRequest(state.generateExternalMessageId(), expectedSuccesorId.getValueAsByteArray()),
                Duration.ofSeconds(10L),
                FindSuccessorResponse.class);
        state.failIfSelf(fsr);
        ExternalPointer successor = state.toExternalPointer(fsr, bootstrapNode.getAddress());
        fingerTable.put(successor);

        // get our successor's pred 
        GetPredecessorResponse gpr = funnelToRequestCoroutine(
                cnt,
                successor.getAddress(),
                new GetPredecessorRequest(state.generateExternalMessageId()),
                Duration.ofSeconds(10L),
                GetPredecessorResponse.class);
        state.failIfSelf(gpr);

        // populate fingertable
        for (int i = 1; i < len; i++) {
            // successor may extend multiple slots in to the fingertable... if it does, skip this entry and go to the next
            if (fingerTable.get(i) instanceof ExternalPointer) {
                LOG.warn("No need to find finger for index {}", i);
                continue;
            }

            // get expected id of entry in finger table
            NodeId findId = fingerTable.getExpectedId(i);

            // route to id if possible... if failed to route, skip this entry and go to the next (this will repair eventually once this node
            // is up)
            try {
                fsr = funnelToRequestCoroutine(
                        cnt,
                        bootstrapNode.getAddress(),
                        new FindSuccessorRequest(state.generateExternalMessageId(), findId.getValueAsByteArray()),
                        Duration.ofSeconds(10L),
                        FindSuccessorResponse.class);
            } catch (RuntimeException coe) {
                LOG.warn("Unable to find finger for index {}", i);
                continue;
            }

            // set in to finger table
            ExternalPointer foundFinger = state.toExternalPointer(fsr, bootstrapNode.getAddress());
            fingerTable.put(foundFinger);
        }

        // create successor table and sync to finger table
        SuccessorTable successorTable = new SuccessorTable(state.getSelfPointer());
        successorTable.updateTrim(fingerTable.get(0));

        state.setTables(fingerTable, successorTable);
        state.setPredecessor(gpr);
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
