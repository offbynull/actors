package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.FindSuccessorResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse.ExternalSuccessorEntry;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse.InternalSuccessorEntry;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse.SuccessorEntry;
import com.offbynull.peernetic.playground.chorddht.shared.ChordUtils;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class RemoteRouteToTask<A> extends ChordTask<A> {
    private final Id findId;
    private final Object originalRequest;
    private final Endpoint originalSource;
    
    
    public static <A> RemoteRouteToTask<A> create(Instant time, ChordContext<A> context, Id findId,
            Object originalRequest, Endpoint originalSource) throws Exception {
        // create
        RemoteRouteToTask<A> task = new RemoteRouteToTask<>(context, findId, originalRequest, originalSource);
        JavaflowActor actor = new JavaflowActor(task);
        task.initialize(time, actor);

        return task;
    }
    
    private RemoteRouteToTask(ChordContext<A> context, Id findId, Object originalRequest, Endpoint originalSource) {
        super(context);
        
        Validate.notNull(findId);
        Validate.notNull(originalRequest);
        Validate.notNull(originalSource);

        this.findId = findId;
        this.originalRequest = originalRequest;
        this.originalSource = originalSource;
    }
    
    @Override
    public void execute() throws Exception {
        // find predecessor
        RouteToTask<A> routeToTask = RouteToTask.create(getTime(), getContext(), findId);
        getFlowControl().waitUntilFinished(routeToTask.getActor(), Duration.ofSeconds(1L));
        Pointer foundSucc = routeToTask.getResult();

        if (foundSucc == null) {
            throw new IllegalArgumentException();
        }

        Id foundId;
        A foundAddress = null;

        boolean isInternalPointer = foundSucc instanceof InternalPointer;
        boolean isExternalPointer = foundSucc instanceof ExternalPointer;

        if (isInternalPointer) {
            Pointer successor = getContext().getFingerTable().get(0);

            if (successor instanceof InternalPointer) {
                foundId = successor.getId(); // id will always be the same as us
            } else if (successor instanceof ExternalPointer) {
                ExternalPointer<A> externalSuccessor = (ExternalPointer<A>) successor;
                foundId = externalSuccessor.getId();
                foundAddress = externalSuccessor.getAddress();
            } else {
                throw new IllegalStateException();
            }
        } else if (isExternalPointer) {
            ExternalPointer<A> externalPred = (ExternalPointer<A>) foundSucc;

            GetSuccessorResponse<A> gsr = getFlowControl().sendRequestAndWait(new GetSuccessorRequest(), externalPred.getAddress(),
                    GetSuccessorResponse.class, Duration.ofSeconds(3L));

            SuccessorEntry successorEntry = gsr.getEntries().get(0);

            A senderAddress = getContext().getEndpointIdentifier().identify(getSource());
            A address;
            if (successorEntry instanceof InternalSuccessorEntry) { // this means the successor to the node is itself
                address = senderAddress;
            } else if (successorEntry instanceof ExternalSuccessorEntry) {
                address = ((ExternalSuccessorEntry<A>) successorEntry).getAddress();
            } else {
                throw new IllegalStateException();
            }

            // ask for that successor's id, wait for response here
            GetIdResponse gir = getFlowControl().sendRequestAndWait(new GetIdRequest(), address, GetIdResponse.class, Duration.ofSeconds(3L));

            int bitSize = ChordUtils.getBitLength(findId);
            foundId = new Id(gir.getId(), bitSize);
            foundAddress = getContext().getEndpointIdentifier().identify(getSource());
        } else {
            throw new IllegalStateException();
        }

        FindSuccessorResponse<A> response = new FindSuccessorResponse<>(foundId.getValueAsByteArray(), foundAddress);
        getContext().getRouter().sendResponse(getTime(), originalRequest, response, originalSource);
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
