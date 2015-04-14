package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.externalmessages.GetPredecessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetPredecessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.ExternalSuccessorEntry;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.InternalSuccessorEntry;
import com.offbynull.peernetic.examples.chord.externalmessages.NotifyRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.UpdateFingerTableRequest;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.coroutines.ParentCoroutine;
import com.offbynull.peernetic.examples.common.coroutines.SendRequestCoroutine;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StabilizeTask implements Coroutine {
    
    private static final Logger LOG = LoggerFactory.getLogger(StabilizeTask.class);
    
    private final String sourceId;
    private final State state;

    public StabilizeTask(String sourceId, State state) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        this.sourceId = sourceId;
        this.state = state;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        NodeId selfId = state.getSelfId();
        
        Context ctx = (Context) cnt.getContext();
        ParentCoroutine parentCoroutine = new ParentCoroutine(sourceId, state.getTimerPrefix(), ctx);
        
        while (true) {
            parentCoroutine.addSleep(Duration.ofSeconds(1L));
            parentCoroutine.run(cnt);
            
            try {
                Pointer successor = state.getSuccessor();
                if (state.isSelfId(successor.getId())) {
                    continue;
                }

                // ask for successor's pred
                String successorAddress = ((ExternalPointer) successor).getAddress();
                
                long msgId;
                SendRequestCoroutine sendRequestCoroutine;
                
                msgId = state.generateExternalMessageId();
                sendRequestCoroutine = parentCoroutine.addSendRequest(
                        successorAddress,
                        new GetPredecessorRequest(msgId),
                        Duration.ofSeconds(10L),
                        GetPredecessorResponse.class);
                parentCoroutine.run(cnt);
                GetPredecessorResponse gpr = sendRequestCoroutine.getResponse();

                // check to see if predecessor is between us and our successor
                if (gpr.getChordId() != null) {
                    String address = gpr.getAddress();
                    NodeId potentiallyNewSuccessorId = state.toId(gpr.getChordId());
                    NodeId existingSuccessorId = ((ExternalPointer) successor).getId();

                    if (potentiallyNewSuccessorId.isWithin(selfId, false, existingSuccessorId, false)) {
                        // it is between us and our successor, so set it and notify it that we are its predecessor
                        ExternalPointer newSuccessor = new ExternalPointer(potentiallyNewSuccessorId, address);
                        state.setSuccessor(newSuccessor);
                        
                        msgId = state.generateExternalMessageId();
                        ctx.addOutgoingMessage(
                                AddressUtils.parentize(sourceId, "" + msgId),
                                newSuccessor.getAddress(),
                                new NotifyRequest(msgId, selfId.getValueAsByteArray()));
                        
                        successor = newSuccessor;
                        successorAddress = newSuccessor.getAddress();
                    }
                }

                // successor may have been updated by block above
                // ask successor for its successors
                msgId = state.generateExternalMessageId();
                sendRequestCoroutine = parentCoroutine.addSendRequest(
                        successorAddress,
                        new GetSuccessorRequest(msgId),
                        Duration.ofSeconds(10L),
                        GetSuccessorResponse.class);
                parentCoroutine.run(cnt);
                GetSuccessorResponse gsr = sendRequestCoroutine.getResponse();

                List<Pointer> subsequentSuccessors = new ArrayList<>();
                gsr.getEntries().stream().map(x -> {
                    NodeId id = state.toId(x.getChordId());

                    if (x instanceof InternalSuccessorEntry) {
                        return new InternalPointer(id);
                    } else if (x instanceof ExternalSuccessorEntry) {
                        return new ExternalPointer(id, ((ExternalSuccessorEntry) x).getAddress());
                    } else {
                        throw new IllegalStateException();
                    }
                }).forEachOrdered(x -> subsequentSuccessors.add(x));

                // mark it as our new successor
                state.updateSuccessor((ExternalPointer) successor, subsequentSuccessors);
            } catch (RuntimeException re) {
                LOG.warn("Failed to stabilize", re);
            }
        }
    }
}
