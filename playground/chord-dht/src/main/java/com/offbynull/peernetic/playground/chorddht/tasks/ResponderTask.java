package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.javaflow.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.FindSuccessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestFingerRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestFingerResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetPredecessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetPredecessorResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.NotifyRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.NotifyResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.UpdateFingerTableRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.UpdateFingerTableResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Instant;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class ResponderTask<A> extends SimpleJavaflowTask<A, byte[]> {
    
    private final ChordContext<A> context;

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
                context.getRouter().sendResponse(getTime(), message, new GetIdResponse(context.getSelfId().getValueAsByteArray()),
                        getSource());
            } else if (message instanceof GetClosestFingerRequest) {
                GetClosestFingerRequest request = (GetClosestFingerRequest) message;
                Id id = new Id(request.getId(), context.getSelfId().getLimitAsByteArray());
                Id skipId = new Id(request.getSkipId(), context.getSelfId().getLimitAsByteArray());
                Pointer pointer = context.getFingerTable().findClosest(id, skipId);
                ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
                context.getRouter().sendResponse(getTime(), message,
                        new GetClosestFingerResponse<>(msgValues.getLeft(), msgValues.getRight()), getSource());
            } else if (message instanceof GetClosestPrecedingFingerRequest) {
                GetClosestPrecedingFingerRequest request = (GetClosestPrecedingFingerRequest) message;
                Id id = new Id(request.getId(), context.getSelfId().getLimitAsByteArray());
                Pointer pointer = context.getFingerTable().findClosestPreceding(id);
                ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
                context.getRouter().sendResponse(getTime(), message,
                        new GetClosestPrecedingFingerResponse<>(msgValues.getLeft(), msgValues.getRight()), getSource());
            } else if (message instanceof GetPredecessorRequest) {
                Pointer pointer = context.getPredecessor();
                ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
                context.getRouter().sendResponse(getTime(), message,
                        new GetPredecessorResponse<>(msgValues.getLeft(), msgValues.getRight()), getSource());
            } else if (message instanceof GetSuccessorRequest) {
                List<Pointer> pointers = context.getSuccessorTable().dump();
                context.getRouter().sendResponse(getTime(), message,
                        new GetSuccessorResponse<>(pointers), getSource());
            } else if (message instanceof NotifyRequest) {
                NotifyRequest request = (NotifyRequest) message;
                byte[] idBytes = request.getId();
                Id id = new Id(idBytes, context.getSelfId().getLimitAsByteArray());

                ExternalPointer<A> newPredecessor
                        = new ExternalPointer<>(id, context.getEndpointIdentifier().identify(getSource()));
                ExternalPointer<A> existingPredecessor = (ExternalPointer<A>) context.getPredecessor();
                if (context.getPredecessor() == null || id.isWithin(existingPredecessor.getId(), true, context.getSelfId(), false)) {
                    context.setPredecessor(newPredecessor);
                }

                Pointer pointer = context.getPredecessor();
                ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
                context.getRouter().sendResponse(getTime(), message,
                        new NotifyResponse<>(msgValues.getLeft(), msgValues.getRight()), getSource());
            } else if (message instanceof UpdateFingerTableRequest) {
                UpdateFingerTableRequest request = (UpdateFingerTableRequest) message;
                byte[] idBytes = request.getId();
                Id id = new Id(idBytes, context.getSelfId().getLimitAsByteArray());
                ExternalPointer<A> newFinger
                        = new ExternalPointer<>(id, context.getEndpointIdentifier().identify(getSource()));

                if (!id.equals(context.getSelfId())) {
                    boolean replaced = context.getFingerTable().replace(newFinger);
                    if (replaced && context.getPredecessor() != null) {
                        getFlowControl().sendRequest(new UpdateFingerTableRequest(idBytes), context.getPredecessor().getAddress());
                    }
                }

                context.getRouter().sendResponse(getTime(), message, new UpdateFingerTableResponse(), getSource());
            } else if (message instanceof FindSuccessorRequest) {
                FindSuccessorRequest request = (FindSuccessorRequest) message;
                byte[] idBytes = request.getId();
                Id id = new Id(idBytes, context.getSelfId().getLimitAsByteArray());

                try {
                    // we don't want to block this task by waiting for remoteroutetosuccessor to complete
                    RemoteRouteToTask.create(getTime(), context, id, request, getSource());
                } catch (Exception e) {
                    // should never happen
                }
            }
        }
    }
    
    private ImmutablePair<byte[], A> convertPointerToMessageDetails(Pointer pointer) {
        if (pointer == null) {
            return new ImmutablePair<>(null, null);
        }

        byte[] idBytes = pointer.getId().getValueAsByteArray();
        if (pointer instanceof InternalPointer) {
            return new ImmutablePair<>(idBytes, null);
        } else if (pointer instanceof ExternalPointer) {
            A address = ((ExternalPointer<A>) pointer).getAddress();
            return new ImmutablePair<>(idBytes, address);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
