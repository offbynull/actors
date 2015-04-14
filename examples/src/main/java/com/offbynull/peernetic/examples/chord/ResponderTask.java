package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.examples.chord.externalmessages.FindSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestFingerRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetPredecessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.NotifyRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.UpdateFingerTableRequest;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ResponderTask implements Coroutine {
    
    private static final Logger LOG = LoggerFactory.getLogger(ResponderTask.class);

    private final String sourceNodeId;
    private final State state;
    private final TaskHelper taskHelper;

    public ResponderTask(String sourceNodeId, State state, TaskHelper taskHelper) {
        Validate.notNull(sourceNodeId);
        Validate.notNull(state);
        Validate.notNull(taskHelper);
        this.sourceNodeId = sourceNodeId;
        this.state = state;
        this.taskHelper = taskHelper;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        while (true) {
            cnt.suspend();
            
            Object message = null;
//            Object message = getFlowControl().waitForRequest(
//                    GetNodeIdRequest.class,
//                    GetClosestPrecedingFingerRequest.class,
//                    GetClosestFingerRequest.class,
//                    GetPredecessorRequest.class,
//                    GetSuccessorRequest.class,
//                    NotifyRequest.class,
//                    UpdateFingerTableRequest.class,
//                    FindSuccessorRequest.class);
            
            

            LOG.debug("Incoming request {}", message.getClass());
            
            if (message instanceof GetNodeIdRequest) {
                taskHelper.trackRequest(message);
                GetNodeIdRequest request = (GetNodeIdRequest) message;
                taskHelper.sendGetNodeIdResponse(request, getSource(), state.getSelfId());
            } else if (message instanceof GetClosestFingerRequest) {
                taskHelper.trackRequest(message);
                GetClosestFingerRequest request = (GetClosestFingerRequest) message;
                Pointer pointer = state.getClosestFinger(request);
                taskHelper.sendGetClosestFingerResponse(request, getSource(), pointer);
            } else if (message instanceof GetClosestPrecedingFingerRequest) {
                taskHelper.trackRequest(message);
                GetClosestPrecedingFingerRequest request = (GetClosestPrecedingFingerRequest) message;
                Pointer pointer = state.getClosestPrecedingFinger(request);
                taskHelper.sendGetClosestPrecedingFingerResponse(request, getSource(), pointer);
            } else if (message instanceof GetPredecessorRequest) {
                taskHelper.trackRequest(message);
                GetPredecessorRequest request = (GetPredecessorRequest) message;
                ExternalPointer pointer = state.getPredecessor();
                taskHelper.sendGetPredecessorResponse(request, getSource(), pointer);
            } else if (message instanceof GetSuccessorRequest) {
                taskHelper.trackRequest(message);
                GetSuccessorRequest request = (GetSuccessorRequest) message;
                List<Pointer> successors = state.getSuccessors();
                taskHelper.sendGetSuccessorResponse(request, getSource(), successors);
            } else if (message instanceof NotifyRequest) {
                taskHelper.trackRequest(message);
                NotifyRequest request = (NotifyRequest) message;
                NodeId id = state.toId(request.getChordId());

                ExternalPointer newPredecessor = new ExternalPointer(id, taskHelper.getCurrentMessageAddress());
                ExternalPointer existingPredecessor = state.getPredecessor();
                if (existingPredecessor == null || id.isWithin(existingPredecessor.getId(), true, state.getSelfId(), false)) {
                    state.setPredecessor(newPredecessor);
                }

                ExternalPointer pointer = state.getPredecessor();
                taskHelper.sendNotifyResponse(request, getSource(), pointer);
            } else if (message instanceof UpdateFingerTableRequest) {
                taskHelper.trackRequest(message);
                UpdateFingerTableRequest request = (UpdateFingerTableRequest) message;
                NodeId id = state.toId(request.getChordId());
                ExternalPointer newFinger = new ExternalPointer(id, taskHelper.getCurrentMessageAddress());

                if (!state.isSelfId(id)) {
                    boolean replaced = state.replaceFinger(newFinger);
                    ExternalPointer pred = state.getPredecessor();
                    if (replaced && pred != null) {
                        taskHelper.fireUpdateFingerTableRequest(pred.getAddress(), id);
                    }
                }

                taskHelper.sendUpdateFingerTableResponse(request, getSource());
            } else if (message instanceof FindSuccessorRequest) {
                taskHelper.trackRequestLong(message);
                FindSuccessorRequest request = (FindSuccessorRequest) message;
                NodeId id = state.toId(request.getChordId());

                try {
                    // we don't want to block the responder task by waiting for remoterouteto to complete
                    // response sent from within task
                    taskHelper.fireRemoteRouteToTask(id, request, getSource());
                } catch (Exception e) {
                    // should never happen
                }
            }
        }
    }
}
