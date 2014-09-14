package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.BaseContinuableTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.ContinuationActor;
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
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class RemoteRouteToSuccessorTask<A> extends BaseContinuableTask<A, byte[]> {
    private final ChordContext<A> context;
    private final Id findId;
    private final Object originalRequest;
    private final Endpoint originalSource;
    
    
    public static <A> RemoteRouteToSuccessorTask<A> createAndAssignToRouter(Instant time, ChordContext<A> context, Id findId,
            Object originalRequest, Endpoint originalSource) throws Exception {
        // create
        RemoteRouteToSuccessorTask<A> task = new RemoteRouteToSuccessorTask<>(context, findId, originalRequest, originalSource);
        ContinuationActor encapsulatingActor = new ContinuationActor(task);
        task.setEncapsulatingActor(encapsulatingActor);
        
        // register types here

        // prime
        encapsulatingActor.onStep(time, NullEndpoint.INSTANCE, new InternalStart());
        
        return task;
    }
    
    public static <A> void unassignFromRouter(ChordContext<A> context, RemoteRouteToSuccessorTask<A> task) {
        context.getRouter().removeActor(task.getEncapsulatingActor());
    }
    
    private RemoteRouteToSuccessorTask(ChordContext<A> context, Id findId, Object originalRequest, Endpoint originalSource) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        
        Validate.notNull(context);
        Validate.notNull(findId);
        Validate.notNull(originalRequest);
        Validate.notNull(originalSource);

        this.context = context;
        this.findId = findId;
        this.originalRequest = originalRequest;
        this.originalSource = originalSource;
    }
    
    @Override
    public void run() {
        try {
            scheduleTimer();
            
            // find predecessor
            RouteToSuccessorTask<A> routeToPredTask = RouteToSuccessorTask.createAndAssignToRouter(getTime(), context, findId);
            waitUntilFinished(routeToPredTask.getEncapsulatingActor());
            Pointer foundSucc = routeToPredTask.getResult();
            
            if (foundSucc == null) {
                throw new IllegalArgumentException();
            }
            
            Id foundId;
            A foundAddress = null;
            if (foundSucc instanceof InternalPointer) {
                Pointer successor = context.getFingerTable().get(0);
                
                if (successor instanceof InternalPointer) {
                    foundId = successor.getId(); // id will always be the same as us
                } else if (successor instanceof ExternalPointer) {
                    ExternalPointer<A> externalSuccessor = (ExternalPointer<A>) successor;
                    foundId = externalSuccessor.getId();
                    foundAddress = externalSuccessor.getAddress();
                } else {
                    throw new IllegalArgumentException();
                }
            } else if (foundSucc instanceof ExternalPointer) {
                ExternalPointer<A> externalPred = (ExternalPointer<A>) foundSucc;
                
                GetSuccessorResponse<A> gsr = sendAndWaitUntilResponse(new GetSuccessorRequest(), externalPred.getAddress(),
                    GetSuccessorResponse.class);
                
                SuccessorEntry successorEntry = gsr.getEntries().get(0);

                A senderAddress = context.getEndpointIdentifier().identify(getSource());
                A address;
                if (successorEntry instanceof InternalSuccessorEntry) { // this means the successor to the node is itself
                    address = senderAddress;
                } else if (successorEntry instanceof ExternalSuccessorEntry) {
                    address = ((ExternalSuccessorEntry<A>) successorEntry).getAddress();
                } else {
                    throw new IllegalArgumentException();
                }

                // ask for that successor's id, wait for response here
                GetIdResponse gir = sendAndWaitUntilResponse(new GetIdRequest(), address, GetIdResponse.class);

                int bitSize = ChordUtils.getBitLength(findId);
                foundId = new Id(gir.getId(), bitSize);
                foundAddress = context.getEndpointIdentifier().identify(getSource());
            } else {
                throw new IllegalArgumentException();
            }
            
            FindSuccessorResponse<A> response = new FindSuccessorResponse<>(foundId.getValueAsByteArray(), foundAddress);
            context.getRouter().sendResponse(getTime(), originalRequest, response, originalSource);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            unassignFromRouter(context, this);
        }
    }
    
    private static final class InternalStart {
        
    }
}
