package com.offbynull.peernetic.examples.chord;


import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.time.Duration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CheckPredecessorTask implements Subcoroutine<Void> {
    
    private static final Logger LOG = LoggerFactory.getLogger(CheckPredecessorTask.class);

    private final String sourceId;
    private final State state;

    public CheckPredecessorTask(String sourceId, State state) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        this.sourceId = sourceId;
        this.state = state;
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
            
            // ask for our predecessor's id
            GetIdResponse gir;
            try {
                gir = funnelToRequestCoroutine(
                        cnt,
                        predecessor.getAddress(),
                        new GetIdRequest(state.generateExternalMessageId()),
                        GetIdResponse.class);
            } catch (RuntimeException re) {
                // predecessor didn't respond -- clear our predecessor
                state.clearPredecessor();
                continue;
            }
            
            NodeId id = gir.getChordId();
            // TODO: Is it worth checking to see if this new id is between the old id and the us? if it is, set it as the new pred???
            if (!id.equals(predecessor.getId())) {
                // predecessor responded with unexpected id -- clear our predecessor
                state.clearPredecessor();
            }
        }
    }
    
    @Override
    public String getId() {
        return sourceId;
    }
    
    private void funnelToSleepCoroutine(Continuation cnt, Duration duration) throws Exception {
        new SleepSubcoroutine.Builder()
                .id(sourceId)
                .duration(duration)
                .timerAddressPrefix(state.getTimerPrefix())
                .build()
                .run(cnt);
    }

    private <T extends ExternalMessage> T funnelToRequestCoroutine(Continuation cnt, String destination, ExternalMessage message,
            Class<T> expectedResponseClass) throws Exception {
        RequestSubcoroutine<T> requestSubcoroutine = new RequestSubcoroutine.Builder<T>()
                .id(AddressUtils.parentize(sourceId, "" + message.getId()))
                .destinationAddress(destination)
                .request(message)
                .timerAddressPrefix(state.getTimerPrefix())
                .addExpectedResponseType(expectedResponseClass)
                .build();
        return requestSubcoroutine.run(cnt);
    }
}
