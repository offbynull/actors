package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.examples.chord.externalmessages.FindSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.ExternalSuccessorEntry;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.InternalSuccessorEntry;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.SuccessorEntry;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RemoteRouteToTask implements Coroutine {
    
    private static final Logger LOG = LoggerFactory.getLogger(RemoteRouteToTask.class);

    private final NodeId findId;
    private final FindSuccessorRequest originalRequest;
    private final String originalSource;
    
    private final String sourceId;
    private final State state;
    private final TaskHelper taskHelper;

    public RemoteRouteToTask(String sourceId, State state, TaskHelper taskHelper, NodeId findId, FindSuccessorRequest originalRequest,
            String originalSource) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        Validate.notNull(taskHelper);
        Validate.notNull(findId);
        Validate.notNull(originalRequest);
        Validate.notNull(originalSource);
        this.sourceId = sourceId;
        this.state = state;
        this.taskHelper = taskHelper;
        this.findId = findId;
        this.originalRequest = originalRequest;
        this.originalSource = originalSource;
    }
    
    @Override
    public void run(Continuation cnt) throws Exception {
        Pointer foundSucc;
        try {
            foundSucc = taskHelper.runRouteToTask(findId);
        } catch (RuntimeException coe) {
            LOG.warn("Unable to route to node");
            return;
        }

        NodeId foundId;
        String foundAddress = null;

        boolean isInternalPointer = foundSucc instanceof InternalPointer;
        boolean isExternalPointer = foundSucc instanceof ExternalPointer;

        if (isInternalPointer) {
            Pointer successor = state.getSuccessor();

            if (successor instanceof InternalPointer) {
                foundId = successor.getId(); // id will always be the same as us
            } else if (successor instanceof ExternalPointer) {
                ExternalPointer externalSuccessor = (ExternalPointer) successor;
                foundId = externalSuccessor.getId();
                foundAddress = externalSuccessor.getAddress();
            } else {
                throw new IllegalStateException();
            }
        } else if (isExternalPointer) {
            try {
                ExternalPointer externalPred = (ExternalPointer) foundSucc;
                GetSuccessorResponse gsr = taskHelper.sendGetSuccessorRequest(cnt, sourceId, externalPred.getAddress());
                SuccessorEntry successorEntry = gsr.getEntries().get(0);

                String senderAddress = chordHelper.getCurrentMessageAddress();
                String address;
                if (successorEntry instanceof InternalSuccessorEntry) { // this means the successor to the node is itself
                    address = senderAddress;
                } else if (successorEntry instanceof ExternalSuccessorEntry) {
                    address = ((ExternalSuccessorEntry) successorEntry).getAddress();
                } else {
                    throw new IllegalStateException();
                }

                // ask for that successor's id, wait for response here
                GetIdResponse gir = chordHelper.sendGetIdRequest(address);
                foundId = state.toId(gir.getChordId());
                foundAddress = chordHelper.getCurrentMessageAddress();
            } catch (RuntimeException coe) {
                LOG.warn("Unable to get successor of node routed to.");
                return;
            }
        } else {
            throw new IllegalStateException();
        }

        chordHelper.sendFindSuccessorResponse(originalRequest, originalSource, foundId, foundAddress);
    }
}
