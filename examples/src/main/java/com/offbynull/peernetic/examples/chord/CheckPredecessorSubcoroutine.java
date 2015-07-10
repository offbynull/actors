package com.offbynull.peernetic.examples.chord;


import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.chord.AddressConstants.ROUTER_HANDLER_RELATIVE_ADDRESS;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import java.time.Duration;
import org.apache.commons.lang3.Validate;

final class CheckPredecessorSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    
    private final State state;
    private final Address timerAddress;

    public CheckPredecessorSubcoroutine(Address subAddress, State state, Address timerAddress) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        Validate.notNull(timerAddress);
        this.subAddress = subAddress;
        this.state = state;
        this.timerAddress = timerAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        
        Context ctx = (Context) cnt.getContext();
        
        while (true) {
            funnelToSleepCoroutine(cnt, Duration.ofSeconds(1L));
                        
            ExternalPointer predecessor = state.getPredecessor();
            if (predecessor == null) {
                // we don't have a predecessor to check
                continue;
            }
            
            // ask for our predecessor's address
            GetIdResponse gir;
            try {
                gir = funnelToRequestCoroutine(
                        cnt,
                        predecessor.getLinkId(),
                        new GetIdRequest(),
                        GetIdResponse.class);
            } catch (RuntimeException re) {
                // predecessor didn't respond -- clear our predecessor
                state.clearPredecessor();
                continue;
            }
            
            NodeId id = gir.getChordId();
            // TODO: Is it worth checking to see if this new address is between the old address and the us? if it is, set it as the new pred???
            if (!id.equals(predecessor.getId())) {
                // predecessor responded with unexpected address -- clear our predecessor
                state.clearPredecessor();
            }
        }
    }
    
    @Override
    public Address getAddress() {
        return subAddress;
    }
    
    private void funnelToSleepCoroutine(Continuation cnt, Duration duration) throws Exception {
        new SleepSubcoroutine.Builder()
                .address(subAddress)
                .duration(duration)
                .timerAddressPrefix(timerAddress)
                .build()
                .run(cnt);
    }

    private <T> T funnelToRequestCoroutine(Continuation cnt, String destinationLinkId, Object message,
            Class<T> expectedResponseClass) throws Exception {
        Address destination = state.getAddressTransformer().linkIdToRemoteAddress(destinationLinkId);
        RequestSubcoroutine<T> requestSubcoroutine = new RequestSubcoroutine.Builder<T>()
                .address(subAddress.appendSuffix(state.nextRandomId()))
                .destinationAddress(destination.appendSuffix(ROUTER_HANDLER_RELATIVE_ADDRESS))
                .request(message)
                .timerAddressPrefix(timerAddress)
                .addExpectedResponseType(expectedResponseClass)
                .build();
        return requestSubcoroutine.run(cnt);
    }
}
