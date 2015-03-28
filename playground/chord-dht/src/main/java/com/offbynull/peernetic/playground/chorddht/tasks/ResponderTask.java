package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.CoroutineActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.skeleton.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.FindSuccessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestFingerRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetPredecessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.NotifyRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.UpdateFingerTableRequest;
import com.offbynull.peernetic.playground.chorddht.shared.ChordHelper;
import com.offbynull.peernetic.playground.chorddht.model.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.model.Pointer;
import java.time.Instant;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResponderTask<A> extends SimpleJavaflowTask<A, byte[]> {
    
    private static final Logger LOG = LoggerFactory.getLogger(ResponderTask.class);
    
    private final ChordHelper<A, byte[]> chordHelper;

    public static <A> ResponderTask<A> create(Instant time, ChordContext<A> context) throws Exception {
        // create
        ResponderTask<A> task = new ResponderTask<>(context);
        CoroutineActor actor = new CoroutineActor(task);
        task.initialize(time, actor);

        return task;
    }

    private ResponderTask(ChordContext<A> context) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        
        Validate.notNull(context);
        this.chordHelper = new ChordHelper<>(getState(), getFlowControl(), context);
    }

    @Override
    public void execute() throws Exception {
        while (true) {
            Object message = getFlowControl().waitForRequest(
                    GetIdRequest.class,
                    GetClosestPrecedingFingerRequest.class,
                    GetClosestFingerRequest.class,
                    GetPredecessorRequest.class,
                    GetSuccessorRequest.class,
                    NotifyRequest.class,
                    UpdateFingerTableRequest.class,
                    FindSuccessorRequest.class);

            LOG.debug("Incoming request {}", message.getClass());
            
            if (message instanceof GetIdRequest) {
                chordHelper.trackRequest(message);
                GetIdRequest request = (GetIdRequest) message;
                chordHelper.sendGetIdResponse(request, getSource(), chordHelper.getSelfId());
            } else if (message instanceof GetClosestFingerRequest) {
                chordHelper.trackRequest(message);
                GetClosestFingerRequest request = (GetClosestFingerRequest) message;
                Pointer pointer = chordHelper.getClosestFinger(request);
                chordHelper.sendGetClosestFingerResponse(request, getSource(), pointer);
            } else if (message instanceof GetClosestPrecedingFingerRequest) {
                chordHelper.trackRequest(message);
                GetClosestPrecedingFingerRequest request = (GetClosestPrecedingFingerRequest) message;
                Pointer pointer = chordHelper.getClosestPrecedingFinger(request);
                chordHelper.sendGetClosestPrecedingFingerResponse(request, getSource(), pointer);
            } else if (message instanceof GetPredecessorRequest) {
                chordHelper.trackRequest(message);
                GetPredecessorRequest request = (GetPredecessorRequest) message;
                ExternalPointer<A> pointer = chordHelper.getPredecessor();
                chordHelper.sendGetPredecessorResponse(request, getSource(), pointer);
            } else if (message instanceof GetSuccessorRequest) {
                chordHelper.trackRequest(message);
                GetSuccessorRequest request = (GetSuccessorRequest) message;
                List<Pointer> successors = chordHelper.getSuccessors();
                chordHelper.sendGetSuccessorResponse(request, getSource(), successors);
            } else if (message instanceof NotifyRequest) {
                chordHelper.trackRequest(message);
                NotifyRequest request = (NotifyRequest) message;
                Id id = chordHelper.toId(request.getId());

                ExternalPointer<A> newPredecessor = new ExternalPointer<>(id, chordHelper.getCurrentMessageAddress());
                ExternalPointer<A> existingPredecessor = chordHelper.getPredecessor();
                if (existingPredecessor == null || id.isWithin(existingPredecessor.getId(), true, chordHelper.getSelfId(), false)) {
                    chordHelper.setPredecessor(newPredecessor);
                }

                ExternalPointer<A> pointer = chordHelper.getPredecessor();
                chordHelper.sendNotifyResponse(request, getSource(), pointer);
            } else if (message instanceof UpdateFingerTableRequest) {
                chordHelper.trackRequest(message);
                UpdateFingerTableRequest request = (UpdateFingerTableRequest) message;
                Id id = chordHelper.toId(request.getId());
                ExternalPointer<A> newFinger = new ExternalPointer<>(id, chordHelper.getCurrentMessageAddress());

                if (!chordHelper.isSelfId(id)) {
                    boolean replaced = chordHelper.replaceFinger(newFinger);
                    ExternalPointer<A> pred = chordHelper.getPredecessor();
                    if (replaced && pred != null) {
                        chordHelper.fireUpdateFingerTableRequest(pred.getAddress(), id);
                    }
                }

                chordHelper.sendUpdateFingerTableResponse(request, getSource());
            } else if (message instanceof FindSuccessorRequest) {
                chordHelper.trackRequestLong(message);
                FindSuccessorRequest request = (FindSuccessorRequest) message;
                Id id = chordHelper.toId(request.getId());

                try {
                    // we don't want to block the responder task by waiting for remoterouteto to complete
                    // response sent from within task
                    chordHelper.fireRemoteRouteToTask(id, request, getSource());
                } catch (Exception e) {
                    // should never happen
                }
            }
        }
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
