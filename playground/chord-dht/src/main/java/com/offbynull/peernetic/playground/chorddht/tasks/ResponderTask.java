package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.javaflow.SimpleJavaflowTask;
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
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Instant;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class ResponderTask<A> extends SimpleJavaflowTask<A, byte[]> {
    
    private final ChordContext<A> context;
    private final ChordHelper<A, byte[]> chordHelper;

    public static <A> ResponderTask<A> create(Instant time, ChordContext<A> context) throws Exception {
        // create
        ResponderTask<A> task = new ResponderTask<>(context);
        JavaflowActor actor = new JavaflowActor(task);
        task.initialize(time, actor);

        return task;
    }

    private ResponderTask(ChordContext<A> context) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        
        Validate.notNull(context);
        this.context = context;
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

            if (message instanceof GetIdRequest) {
                GetIdRequest request = (GetIdRequest) message;
                chordHelper.sendGetIdResponse(request, getSource(), context.getSelfId());
            } else if (message instanceof GetClosestFingerRequest) {
                GetClosestFingerRequest request = (GetClosestFingerRequest) message;
                Id id = chordHelper.convertToId(request.getId());
                Id skipId = chordHelper.convertToId(request.getSkipId());
                Pointer pointer = context.getFingerTable().findClosest(id, skipId);
                chordHelper.sendGetClosestFingerResponse(request, getSource(), pointer);
            } else if (message instanceof GetClosestPrecedingFingerRequest) {
                GetClosestPrecedingFingerRequest request = (GetClosestPrecedingFingerRequest) message;
                Id id = chordHelper.convertToId(request.getId());
                Pointer pointer = context.getFingerTable().findClosestPreceding(id);
                chordHelper.sendGetClosestPrecedingFingerResponse(request, getSource(), pointer);
            } else if (message instanceof GetPredecessorRequest) {
                GetPredecessorRequest request = (GetPredecessorRequest) message;
                ExternalPointer<A> pointer = context.getPredecessor();
                chordHelper.sendGetPredecessorResponse(request, getSource(), pointer);
            } else if (message instanceof GetSuccessorRequest) {
                GetSuccessorRequest request = (GetSuccessorRequest) message;
                List<Pointer> successors = context.getSuccessorTable().dump();
                chordHelper.sendGetSuccessorResponse(request, getSource(), successors);
            } else if (message instanceof NotifyRequest) {
                NotifyRequest request = (NotifyRequest) message;
                Id id = chordHelper.convertToId(request.getId());

                ExternalPointer<A> newPredecessor
                        = new ExternalPointer<>(id, context.getEndpointIdentifier().identify(getSource()));
                ExternalPointer<A> existingPredecessor = (ExternalPointer<A>) context.getPredecessor();
                if (context.getPredecessor() == null || id.isWithin(existingPredecessor.getId(), true, context.getSelfId(), false)) {
                    context.setPredecessor(newPredecessor);
                }

                ExternalPointer<A> pointer = context.getPredecessor();
                chordHelper.sendNotifyResponse(request, getSource(), pointer);
            } else if (message instanceof UpdateFingerTableRequest) {
                UpdateFingerTableRequest request = (UpdateFingerTableRequest) message;
                Id id = chordHelper.convertToId(request.getId());
                ExternalPointer<A> newFinger = new ExternalPointer<>(id, context.getEndpointIdentifier().identify(getSource()));

                if (!id.equals(context.getSelfId())) {
                    boolean replaced = context.getFingerTable().replace(newFinger);
                    if (replaced && context.getPredecessor() != null) {
                        chordHelper.fireUpdateFingerTableRequest(context.getPredecessor().getAddress(), id);
                    }
                }

                chordHelper.sendUpdateFingerTableResponse(request, getSource());
            } else if (message instanceof FindSuccessorRequest) {
                FindSuccessorRequest request = (FindSuccessorRequest) message;
                Id id = chordHelper.convertToId(request.getId());

                try {
                    // we don't want to block this task by waiting for remoteroutetosuccessor to complete
                    RemoteRouteToTask.create(getTime(), context, id, request, getSource());
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
