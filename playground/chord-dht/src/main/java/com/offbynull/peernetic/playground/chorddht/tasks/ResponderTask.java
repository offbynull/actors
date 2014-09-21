package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
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
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class ResponderTask<A> extends ChordTask<A> {

    public static <A> ResponderTask<A> create(Instant time, ChordContext<A> context) throws Exception {
        // create
        ResponderTask<A> task = new ResponderTask<>(context);
        JavaflowActor actor = new JavaflowActor(task);
        task.initialize(time, actor);

        return task;
    }

    private ResponderTask(ChordContext<A> context) {
        super(context);
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
                getContext().getRouter().sendResponse(getTime(), message, new GetIdResponse(getContext().getSelfId().getValueAsByteArray()),
                        getSource());
            } else if (message instanceof GetClosestFingerRequest) {
                GetClosestFingerRequest request = (GetClosestFingerRequest) message;
                Id id = new Id(request.getId(), getContext().getSelfId().getLimitAsByteArray());
                Id skipId = new Id(request.getSkipId(), getContext().getSelfId().getLimitAsByteArray());
                Pointer pointer = getContext().getFingerTable().findClosest(id, skipId);
                ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
                getContext().getRouter().sendResponse(getTime(), message,
                        new GetClosestFingerResponse<>(msgValues.getLeft(), msgValues.getRight()), getSource());
            } else if (message instanceof GetClosestPrecedingFingerRequest) {
                GetClosestPrecedingFingerRequest request = (GetClosestPrecedingFingerRequest) message;
                Id id = new Id(request.getId(), getContext().getSelfId().getLimitAsByteArray());
                Pointer pointer = getContext().getFingerTable().findClosestPreceding(id);
                ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
                getContext().getRouter().sendResponse(getTime(), message,
                        new GetClosestPrecedingFingerResponse<>(msgValues.getLeft(), msgValues.getRight()), getSource());
            } else if (message instanceof GetPredecessorRequest) {
                Pointer pointer = getContext().getPredecessor();
                ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
                getContext().getRouter().sendResponse(getTime(), message,
                        new GetPredecessorResponse<>(msgValues.getLeft(), msgValues.getRight()), getSource());
            } else if (message instanceof GetSuccessorRequest) {
                List<Pointer> pointers = getContext().getSuccessorTable().dump();
                getContext().getRouter().sendResponse(getTime(), message,
                        new GetSuccessorResponse<>(pointers), getSource());
            } else if (message instanceof NotifyRequest) {
                NotifyRequest request = (NotifyRequest) message;
                byte[] idBytes = request.getId();
                Id id = new Id(idBytes, getContext().getSelfId().getLimitAsByteArray());

                ExternalPointer<A> newPredecessor
                        = new ExternalPointer<>(id, getContext().getEndpointIdentifier().identify(getSource()));
                ExternalPointer<A> existingPredecessor = (ExternalPointer<A>) getContext().getPredecessor();
                if (getContext().getPredecessor() == null || id.isWithin(existingPredecessor.getId(), true, getContext().getSelfId(), false)) {
                    getContext().setPredecessor(newPredecessor);
                }

                Pointer pointer = getContext().getPredecessor();
                ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
                getContext().getRouter().sendResponse(getTime(), message,
                        new NotifyResponse<>(msgValues.getLeft(), msgValues.getRight()), getSource());
            } else if (message instanceof UpdateFingerTableRequest) {
                UpdateFingerTableRequest request = (UpdateFingerTableRequest) message;
                byte[] idBytes = request.getId();
                Id id = new Id(idBytes, getContext().getSelfId().getLimitAsByteArray());
                ExternalPointer<A> newFinger
                        = new ExternalPointer<>(id, getContext().getEndpointIdentifier().identify(getSource()));

                if (!id.equals(getContext().getSelfId())) {
                    boolean replaced = getContext().getFingerTable().replace(newFinger);
                    if (replaced && getContext().getPredecessor() != null) {
                        getFlowControl().sendRequest(new UpdateFingerTableRequest(idBytes), getContext().getPredecessor().getAddress());
                    }
                }

                getContext().getRouter().sendResponse(getTime(), message, new UpdateFingerTableResponse(), getSource());
            } else if (message instanceof FindSuccessorRequest) {
                FindSuccessorRequest request = (FindSuccessorRequest) message;
                byte[] idBytes = request.getId();
                Id id = new Id(idBytes, getContext().getSelfId().getLimitAsByteArray());

                try {
                    // we don't want to block this task by waiting for remoteroutetosuccessor to complete
                    RemoteRouteToTask.create(getTime(), getContext(), id, request, getSource());
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
