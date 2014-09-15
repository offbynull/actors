package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.BaseContinuableTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.ContinuationActor;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class RouteToPredecessorTask<A> extends BaseContinuableTask<A, byte[]> {
    private final ChordContext<A> context;
    private final Id findId;
    private ExternalPointer<A> currentNode;
    
    private Id foundId;
    private A foundAddress;
    
    
    public static <A> RouteToPredecessorTask<A> createAndAssignToRouter(Instant time, ChordContext<A> context, Id findId) throws Exception {
        // create
        RouteToPredecessorTask<A> task = new RouteToPredecessorTask<>(context, findId);
        ContinuationActor encapsulatingActor = new ContinuationActor(task);
        task.setEncapsulatingActor(encapsulatingActor);
        
        // register types here

        // prime
        encapsulatingActor.onStep(time, NullEndpoint.INSTANCE, new InternalStart());
        
        return task;
    }
    
    public static <A> void unassignFromRouter(ChordContext<A> context, RouteToPredecessorTask<A> task) {
        context.getRouter().removeActor(task.getEncapsulatingActor());
    }
    
    private RouteToPredecessorTask(ChordContext<A> context, Id findId) {
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
            
            if (findId.isWithin(context.getSelfId(), false, context.getSuccessorTable().getSuccessor().getId(), true)) {
                foundId = context.getSelfId();
                foundAddress = null;
                return;
            }
            
            Pointer initialPointer = context.getFingerTable().findClosestPreceding(findId);
            if (initialPointer instanceof InternalPointer) { // if the closest predecessor is yourself -- which means the ft is empty
                foundId = context.getSelfId();
                foundAddress = null;
                return;
            } else if (initialPointer instanceof ExternalPointer) {
                currentNode = (ExternalPointer<A>) initialPointer;
            } else {
                throw new IllegalStateException();
            }
            
            byte[] findIdData = findId.getValueAsByteArray();
            
            // move forward until you can't move forward anymore
            while (!findId.isWithin(currentNode.getId(), false, querySuccessorId(currentNode), true)) {
                GetClosestPrecedingFingerResponse<A> gcpfr = sendAndWaitUntilResponse(new GetClosestPrecedingFingerRequest(findIdData),
                        currentNode.getAddress(), GetClosestPrecedingFingerResponse.class);

                A address = gcpfr.getAddress();
                byte[] respIdData = gcpfr.getId();
                Id id = new Id(respIdData, currentNode.getId().getLimitAsByteArray());
                

                if (address == null) {
                    currentNode = new ExternalPointer<>(id, currentNode.getAddress());
                } else {
                    currentNode = new ExternalPointer<>(id, address);
                }
            }
            
            foundId = currentNode.getId();
            if (!currentNode.getId().equals(context.getSelfId())) {
                foundAddress = currentNode.getAddress();
            }

        } finally {
            unassignFromRouter(context, this);
        }
    }
    
    private Id querySuccessorId(ExternalPointer<A> pointer) {
        GetSuccessorResponse<A> gsr = sendAndWaitUntilResponse(new GetSuccessorRequest(),
                pointer.getAddress(), GetSuccessorResponse.class);
        return new Id(gsr.getEntries().get(0).getId(), pointer.getId().getLimitAsByteArray());
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
