package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.BaseContinuableTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.ContinuationActor;
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

public final class RouteToSuccessorTask<A> extends BaseContinuableTask<A, byte[]> {
    private final ChordContext<A> context;
    private final Id findId;
    
    private Id foundId;
    private A foundAddress;
    
    
    public static <A> RouteToSuccessorTask<A> createAndAssignToRouter(Instant time, ChordContext<A> context, Id findId) throws Exception {
        // create
        RouteToSuccessorTask<A> task = new RouteToSuccessorTask<>(context, findId);
        ContinuationActor encapsulatingActor = new ContinuationActor(task);
        task.setEncapsulatingActor(encapsulatingActor);
        
        // register types here

        // prime
        encapsulatingActor.onStep(time, NullEndpoint.INSTANCE, new InternalStart());
        
        return task;
    }
    
    public static <A> void unassignFromRouter(ChordContext<A> context, RouteToSuccessorTask<A> task) {
        context.getRouter().removeActor(task.getEncapsulatingActor());
    }
    
    private RouteToSuccessorTask(ChordContext<A> context, Id findId) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        
        Validate.notNull(context);
        Validate.notNull(findId);

        this.context = context;
        this.findId = findId;
    }
    
    @Override
    public void run() {
        try {
            scheduleTimer();
            
            // find predecessor
            RouteToPredecessorTask<A> routeToPredTask = RouteToPredecessorTask.createAndAssignToRouter(getTime(), context, findId);
            waitUntilFinished(routeToPredTask.getEncapsulatingActor());
            Pointer foundPred = routeToPredTask.getResult();
            
            if (foundPred == null) {
                return;
            }
                       
            if (foundPred instanceof InternalPointer) {
                Pointer successor = context.getFingerTable().get(0);
                
                if (successor instanceof InternalPointer) {
                    foundId = successor.getId(); // id will always be the same as us
                } else if (successor instanceof ExternalPointer) {
                    ExternalPointer<A> externalSuccessor = (ExternalPointer<A>) successor;
                    foundId = externalSuccessor.getId();
                    foundAddress = externalSuccessor.getAddress();
                } else {
                    throw new IllegalStateException();
                }
            } else if (foundPred instanceof ExternalPointer) {
                ExternalPointer<A> externalPred = (ExternalPointer<A>) foundPred;
                
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
                    throw new IllegalStateException();
                }

                // ask for that successor's id, wait for response here
                GetIdResponse gir = sendAndWaitUntilResponse(new GetIdRequest(), address, GetIdResponse.class);

                int bitSize = ChordUtils.getBitLength(findId);
                foundId = new Id(gir.getId(), bitSize);
                foundAddress = context.getEndpointIdentifier().identify(getSource());
            } else {
                throw new IllegalStateException();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            unassignFromRouter(context, this);
        }
    }

    public Pointer getResult() {
        if (foundId == null) {
            return null;
        }
        
        if (foundId.equals(context.getSelfId())) {
            return new InternalPointer(foundId);
        }
        
        return new ExternalPointer<>(foundId, foundAddress);
    }
    
    private static final class InternalStart {
        
    }
}
