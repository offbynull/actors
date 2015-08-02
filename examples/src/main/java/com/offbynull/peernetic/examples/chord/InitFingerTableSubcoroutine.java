package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.chord.AddressConstants.ROUTER_HANDLER_RELATIVE_ADDRESS;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.FingerTable;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import com.offbynull.peernetic.examples.chord.model.SuccessorTable;
import org.apache.commons.lang3.Validate;

final class InitFingerTableSubcoroutine implements Subcoroutine<Void> {
    
    private final Address subAddress;
    
    private final State state;
    private final Address timerAddress;
    private final Address logAddress;
    private final IdGenerator idGenerator;
    
    private final String bootstrapLinkId;

    public InitFingerTableSubcoroutine(Address subAddress, State state, String bootstrapLinkId) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        Validate.notNull(bootstrapLinkId);
        this.subAddress = subAddress;
        this.state = state;
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
        this.idGenerator = state.getIdGenerator();
        this.bootstrapLinkId = bootstrapLinkId;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        // NOTE: everything up until the populate finger loop is an essential operation... if a failure occurs, joining has to fail
        FingerTable fingerTable = new FingerTable(state.getSelfPointer());
        SuccessorTable successorTable = new SuccessorTable(state.getSelfPointer());
        
        GetIdResponse gir = funnelToRequestCoroutine(
                cnt,
                bootstrapLinkId,
                new GetIdRequest(),
                GetIdResponse.class);
        ExternalPointer bootstrapNode = state.toExternalPointer(gir.getChordId(), bootstrapLinkId); // fails if address == self
        
        ctx.addOutgoingMessage(subAddress, logAddress, debug("{} {} - Bootstrap node details: {}", state.getSelfId(), subAddress,
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

            ctx.addOutgoingMessage(subAddress, logAddress,
                    debug("{} {} - Found finger at index {} is {}", state.getSelfId(), subAddress, i, foundFinger));
        }

        ctx.addOutgoingMessage(subAddress, logAddress,
                debug("{} {} - Initialization of finger table is complete: {}", state.getSelfId(), subAddress, state.getFingers()));
        
        return null;
    }
    
    @Override
    public Address getAddress() {
        return subAddress;
    }
    
    private <T> T funnelToRequestCoroutine(Continuation cnt, String destinationLinkId, Object message,
            Class<T> expectedResponseClass) throws Exception {
        Address destination = state.getAddressTransformer().linkIdToRemoteAddress(destinationLinkId);
        RequestSubcoroutine<T> requestSubcoroutine = new RequestSubcoroutine.Builder<T>()
                .sourceAddress(subAddress, idGenerator)
                .destinationAddress(destination.appendSuffix(ROUTER_HANDLER_RELATIVE_ADDRESS))
                .request(message)
                .timerAddress(timerAddress)
                .addExpectedResponseType(expectedResponseClass)
                .build();
        return requestSubcoroutine.run(cnt);
    }
    
    private Pointer funnelToInitRouteToSuccessorCoroutine(Continuation cnt, ExternalPointer bootstrapNode, NodeId findId) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(findId);
        
        String idSuffix = idGenerator.generate();
        InitRouteToSuccessorSubcoroutine innerCoroutine = new InitRouteToSuccessorSubcoroutine(
                subAddress.appendSuffix(idSuffix),
                state,
                bootstrapNode,
                findId);
        return innerCoroutine.run(cnt);
    }
}
