package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.BaseContinuableTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.ContinuationActor;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse.ExternalSuccessorEntry;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse.InternalSuccessorEntry;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse.SuccessorEntry;
import com.offbynull.peernetic.playground.chorddht.shared.ChordUtils;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class RouteToPredecessorTask<A> extends BaseContinuableTask<A, byte[]> {
    private final ChordContext<A> context;
    private final Id findId;
    private ExternalPointer<A> currentNode;
    
    private Id foundId;
    private A foundAddress;
    
    
    public static <A> RouteToPredecessorTask<A> createAndAssignToRouter(Instant time, ChordContext<A> context, ExternalPointer<A> initialNode,
            Id findId) throws Exception {
        // create
        RouteToPredecessorTask<A> task = new RouteToPredecessorTask<>(context, initialNode, findId);
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
    
    private RouteToPredecessorTask(ChordContext<A> context, ExternalPointer<A> initialNode, Id findId) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        
        Validate.notNull(context);
        Validate.notNull(initialNode);
        Validate.notNull(findId);

        this.context = context;
        this.findId = findId;
        this.currentNode = initialNode;
    }
    
    @Override
    public void run() {
        try {
            scheduleTimer();
            
            byte[] findIdData = findId.getValueAsByteArray();

            
            // move forward until you can't move forward anymore
            while (true) {
                GetClosestPrecedingFingerResponse<A> gcpfr =  sendAndWaitUntilResponse(new GetClosestPrecedingFingerRequest(findIdData),
                        currentNode.getAddress(), GetClosestPrecedingFingerResponse.class);

                A address = gcpfr.getAddress();
                byte[] respIdData = gcpfr.getId();

                Id id = new Id(respIdData, currentNode.getId().getLimitAsByteArray());

                if (id.equals(currentNode.getId()) && address == null) {
                    // findId's predecessor is the queried node
                    break;
                } else if (!id.equals(currentNode.getId()) && address != null) {
                    ExternalPointer<A> nextNode = new ExternalPointer<>(id, address);
                    currentNode = nextNode;
                    if (findId.isWithin(currentNode.getId(), false, nextNode.getId(), true)) {
                        // node found, stop here
                        break;
                    }// else {
                    //    continue;
                    //}
                } else { // most likely due to: id.equals(currentNode.getId()) && address != null 
                         //    -- this should never happen because if the id the node returns is itself, it gives back a null address
                         //       because it doesn't know what its own address is exposed as
                    throw new IllegalStateException("Bad response from node");
                }
            }
            
            
            // ask for forward-most's successor, wait for response here
            GetIdResponse gir = sendAndWaitUntilResponse(new GetSuccessorRequest(), currentNode.getAddress(),
                    GetIdResponse.class);

            int bitSize = ChordUtils.getBitLength(findId);
            foundId = new Id(gir.getId(), bitSize);
            foundAddress = currentNode.getAddress();
        } finally {
            unassignFromRouter(context, this);
        }
    }

    public ExternalPointer<A> getResult() {
        if (foundId == null || foundAddress == null) {
            return null;
        }
        
        return new ExternalPointer<>(foundId, foundAddress);
    }
    
    private static final class InternalStart {
        
    }
}
