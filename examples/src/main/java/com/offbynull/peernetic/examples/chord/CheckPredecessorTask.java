package com.offbynull.peernetic.examples.chord;


import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.common.coroutines.ParentCoroutine;
import com.offbynull.peernetic.examples.common.coroutines.SendRequestCoroutine;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import java.time.Duration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CheckPredecessorTask implements Coroutine {
    
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
    public void run(Continuation cnt) throws Exception {
        
        Context ctx = (Context) cnt.getContext();
        ParentCoroutine parentCoroutine = new ParentCoroutine(sourceId, state.getTimerPrefix(), ctx);
        
        while (true) {
            parentCoroutine.addSleep(Duration.ofSeconds(1L));
            parentCoroutine.run(cnt);
                        
            ExternalPointer predecessor = state.getPredecessor();
            if (predecessor == null) {
                // we don't have a predecessor to check
                continue;
            }
            
            // ask for our predecessor's id
            GetIdResponse gir;
            try {
                long msgId;
                SendRequestCoroutine sendRequestCoroutine;
                
                msgId = state.generateExternalMessageId();
                sendRequestCoroutine = parentCoroutine.addSendRequest(
                        predecessor.getAddress(),
                        new GetIdRequest(msgId),
                        Duration.ofSeconds(10L),
                        GetIdResponse.class);
                parentCoroutine.run(cnt);
                gir = sendRequestCoroutine.getResponse();
            } catch (RuntimeException re) {
                // predecessor didn't respond -- clear our predecessor
                state.clearPredecessor();
                continue;
            }
            
            NodeId id = state.toId(gir.getChordId());
            // TODO: Is it worth checking to see if this new id is between the old id and the us? if it is, set it as the new pred???
            if (!id.equals(predecessor.getId())) {
                // predecessor responded with unexpected id -- clear our predecessor
                state.clearPredecessor();
            }
        }
    }
}
