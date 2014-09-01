package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.BaseContinuableTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.ContinuationActor;
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
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Instant;
import java.util.List;
import org.apache.commons.javaflow.Continuation;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class ResponderTask<A> extends BaseContinuableTask<A, byte[]> {

    private final ChordContext<A> context;

    public static <A> ResponderTask<A> createAndAssignToRouter(Instant time, ChordContext<A> context) {
        // create
        ResponderTask<A> task = new ResponderTask<>(context);
        ContinuationActor encapsulatingActor = new ContinuationActor(task);
        task.setEncapsulatingActor(encapsulatingActor);

        // register types here
        context.getRouter().addTypeHandler(encapsulatingActor, GetIdRequest.class);
        context.getRouter().addTypeHandler(encapsulatingActor, GetClosestPrecedingFingerRequest.class);
        context.getRouter().addTypeHandler(encapsulatingActor, GetPredecessorRequest.class);
        context.getRouter().addTypeHandler(encapsulatingActor, GetSuccessorRequest.class);
        context.getRouter().addTypeHandler(encapsulatingActor, NotifyRequest.class);
        
        return task;
    }

    public static <A> void unassignFromRouter(ChordContext<A> context, ResponderTask<A> task) {
        context.getRouter().removeActor(task.getEncapsulatingActor());
    }

    private ResponderTask(ChordContext<A> context) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        this.context = context;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Object message = waitUntilType(GetIdRequest.class, GetClosestPrecedingFingerRequest.class, GetPredecessorRequest.class,
                        GetSuccessorRequest.class, NotifyRequest.class);
                
                if (message instanceof GetIdRequest) {
                    context.getRouter().sendResponse(getTime(), message, new GetIdResponse(context.getSelfId().getValueAsByteArray()),
                            getSource());
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

                    ExternalPointer<A> newPredecessor =
                            new ExternalPointer<>(id, context.getEndpointIdentifier().identify(getSource()));
                    ExternalPointer<A> existingPredecessor = (ExternalPointer<A>) context.getPredecessor();
                    if (context.getPredecessor() == null || id.isWithin(existingPredecessor.getId(), true, context.getSelfId(), false)) {
                        context.setPredecessor(newPredecessor);
                    }

                    Pointer pointer = context.getPredecessor();
                    ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
                    context.getRouter().sendResponse(getTime(), message,
                            new NotifyResponse<>(msgValues.getLeft(), msgValues.getRight()), getSource()); 
                }
            }
        } finally {
            unassignFromRouter(context, this);
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
}
