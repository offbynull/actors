package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.FingerTable;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.SuccessorTable;
import org.apache.commons.lang3.Validate;

final class InitFingerTableSubcoroutine implements Subcoroutine<Void> {
    
    private final Address sourceId;
    
    private final State state;
    private final Address timerAddress;
    private final Address logAddress;
    
    private final Address bootstrapAddress;

    public InitFingerTableSubcoroutine(Address sourceId, State state, Address timerAddress, Address logAddress, Address bootstrapAddress) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        Validate.notNull(timerAddress);
        Validate.notNull(logAddress);
        Validate.notNull(bootstrapAddress);
        this.sourceId = sourceId;
        this.state = state;
        this.timerAddress = timerAddress;
        this.logAddress = logAddress;
        this.bootstrapAddress = bootstrapAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        // NOTE: everything up until the populate finger loop is an essential operation... if a failure occurs, joining has to fail
        FingerTable fingerTable = new FingerTable(state.getSelfPointer());
        SuccessorTable successorTable = new SuccessorTable(state.getSelfPointer());
        
        GetIdResponse gir = funnelToRequestCoroutine(
                cnt,
                bootstrapAddress,
                new GetIdRequest(),
                GetIdResponse.class);
        ExternalPointer bootstrapNode = state.toExternalPointer(gir.getChordId(), bootstrapAddress); // fails if id == self
        
        ctx.addOutgoingMessage(sourceId, logAddress, debug("{} {} - Bootstrap node details: {}", state.getSelfId(), sourceId,
                bootstrapNode));
        
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

            ctx.addOutgoingMessage(sourceId, logAddress,
                    debug("{} {} - Found finger at index {} is {}", state.getSelfId(), sourceId, i, foundFinger));
        }

        ctx.addOutgoingMessage(sourceId, logAddress,
                debug("{} {} - Initialization of finger table is complete: {}", state.getSelfId(), sourceId, state.getFingers()));
        
        return null;
    }
    
    @Override
    public Address getId() {
        return sourceId;
    }
    
    private <T> T funnelToRequestCoroutine(Continuation cnt, Address destination, Object message,
            Class<T> expectedResponseClass) throws Exception {
        RequestSubcoroutine<T> requestSubcoroutine = new RequestSubcoroutine.Builder<T>()
                .id(sourceId.appendSuffix(state.nextRandomId()))
                .destinationAddress(destination)
                .request(message)
                .timerAddressPrefix(timerAddress)
                .addExpectedResponseType(expectedResponseClass)
                .build();
        return requestSubcoroutine.run(cnt);
    }
    
    private Pointer funnelToInitRouteToSuccessorCoroutine(Continuation cnt, ExternalPointer bootstrapNode, NodeId findId) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(findId);
        
        String idSuffix = "" + state.nextRandomId();
        InitRouteToSuccessorSubcoroutine innerCoroutine = new InitRouteToSuccessorSubcoroutine(
                sourceId.appendSuffix(idSuffix),
                state,
                timerAddress,
                logAddress,
                bootstrapNode,
                findId);
        return innerCoroutine.run(cnt);
    }
}
