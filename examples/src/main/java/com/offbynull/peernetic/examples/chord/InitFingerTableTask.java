package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.FingerTable;
import com.offbynull.peernetic.examples.chord.model.Pointer;
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

    private final String bootstrapAddress;
    private final String sourceId;
    private final State state;

    public InitFingerTableTask(String sourceId, State state, String bootstrapAddress) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        Validate.notNull(bootstrapAddress);
        this.sourceId = sourceId;
        this.state = state;
        this.bootstrapAddress = bootstrapAddress;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        // NOTE: everything up until the populate finger loop is an essential operation... if a failure occurs, joining has to fail
        FingerTable fingerTable = new FingerTable(state.getSelfPointer());
        SuccessorTable successorTable = new SuccessorTable(state.getSelfPointer());
        
        GetIdResponse gir = funnelToRequestCoroutine(
                cnt,
                bootstrapAddress,
                new GetIdRequest(state.generateExternalMessageId()),
                Duration.ofSeconds(10L),
                GetIdResponse.class);
        ExternalPointer bootstrapNode = state.toExternalPointer(gir.getChordId(), bootstrapAddress); // fails if id == self
        
        LOG.debug("{} {} - Bootstrap node details: {}", state.getSelfId(), sourceId, bootstrapNode);
        
        fingerTable.put(bootstrapNode);
        successorTable.updateTrim(bootstrapNode);
        
        state.setTables(fingerTable, successorTable);
        state.setPredecessor(bootstrapNode);
        
        

        // populate fingertable
        int len = state.getFingerTableLength();
        for (int i = 0; i < len; i++) {
            NodeId findId = state.getExpectedFingerId(i);
            Pointer foundFinger = funnelToInitRouteToSuccessorCoroutine(cnt, bootstrapNode, findId);

            // set in to finger table
            state.validateExternalId(foundFinger);
            fingerTable.put((ExternalPointer) foundFinger);
            
            LOG.debug("{} {} - Found finger at index {} is {}", state.getSelfId(), sourceId, i, foundFinger);
        }
        
        LOG.debug("{} {} - Initialization of finger table is complete: {}", state.getSelfId(), sourceId, state.getFingers());
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
    
    private Pointer funnelToInitRouteToSuccessorCoroutine(Continuation cnt, ExternalPointer bootstrapNode, NodeId findId) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(findId);
        
        String idSuffix = "" + state.generateExternalMessageId();
        InitRouteToSuccessorTask innerCoroutine = new InitRouteToSuccessorTask(
                AddressUtils.parentize(sourceId, idSuffix),
                state,
                bootstrapNode,
                findId);
        innerCoroutine.run(cnt);
        return innerCoroutine.getResult();
    }
}
