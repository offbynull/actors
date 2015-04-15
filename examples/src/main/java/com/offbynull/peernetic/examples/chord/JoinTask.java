package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdResponse;
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

final class JoinTask implements Coroutine {
    
    private static final Logger LOG = LoggerFactory.getLogger(JoinTask.class);

    private final String sourceId;
    private final State state;

    public JoinTask(String sourceId, State state) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        this.sourceId = sourceId;
        this.state = state;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        try {
            // initialize state
            String initialAddress = state.getBootstrapAddress();
            FingerTable fingerTable = new FingerTable(state.getSelfPointer());
            SuccessorTable successorTable = new SuccessorTable(state.getSelfPointer());

            // if no bootstrap address, we're the originator node, so initial successortable+fingertable is what we want.
            if (initialAddress == null) {
                state.setTables(fingerTable, successorTable);
                return;
            }

            // ask for bootstrap node's id
              // retry this req lots of time if it fails -- this is an important step, because it's required for the init finger table task to
              // work properly. it's okay if the init finger table task only resolves some fingers, but it won't be able to resolve any if we
              // don't have our bootstrap's id.
            GetIdResponse gir = funnelToRequestCoroutine(
                    cnt,
                    initialAddress,
                    new GetIdRequest(state.generateExternalMessageId()),
                    Duration.ofSeconds(10L),
                    GetIdResponse.class);
            NodeId initialId = gir.getChordId();

            // init finger table, successor table, etc...
            ExternalPointer bootstrapPointer = new ExternalPointer(initialId, initialAddress);
            funnelToInitFingerTableCoroutine(cnt, bootstrapPointer);
        } catch (RuntimeException coe) {
            // this is a critical operation. if any of the tasks/io fail, then send up the chain
            throw new IllegalStateException("Join failed.", coe);
        }
    }

    private void funnelToInitFingerTableCoroutine(Continuation cnt, ExternalPointer bootstrapPointer) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(bootstrapPointer);
        
        String idSuffix = "" + state.generateExternalMessageId();
        InitFingerTableTask innerCoroutine = new InitFingerTableTask(
                AddressUtils.parentize(sourceId, idSuffix),
                state,
                bootstrapPointer);
        innerCoroutine.run(cnt);
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
