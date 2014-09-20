package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.BaseContinuableTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.ContinuationActor;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestFingerRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestFingerResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class RouteToTask<A> extends BaseContinuableTask<A, byte[]> {
    private final ChordContext<A> context;
    private final Id findId;
    private ExternalPointer<A> currentNode;
    
    private Id foundId;
    private A foundAddress;
    
    
    public static <A> RouteToTask<A> createAndAssignToRouter(Instant time, ChordContext<A> context, Id findId) throws Exception {
        // create
        RouteToTask<A> task = new RouteToTask<>(context, findId);
        ContinuationActor encapsulatingActor = new ContinuationActor(task);
        task.setEncapsulatingActor(encapsulatingActor);
        
        // register types here

        // prime
        encapsulatingActor.onStep(time, NullEndpoint.INSTANCE, new InternalStart());
        
        return task;
    }
    
    public static <A> void unassignFromRouter(ChordContext<A> context, RouteToTask<A> task) {
        context.getRouter().removeActor(task.getEncapsulatingActor());
    }
    
    private RouteToTask(ChordContext<A> context, Id findId) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        
        Validate.notNull(context);
        Validate.notNull(findId);

        this.context = context;
        this.findId = findId;
    }
    
    @Override
    public void execute() throws Exception {
        Pointer initialPointer = context.getFingerTable().findClosest(findId);
        if (initialPointer instanceof InternalPointer) {
            // our finger table may be corrupt/incomplete, try with maximum non-base finger
            initialPointer = context.getFingerTable().getMaximumNonBase();
            if (initialPointer == null) {
                // we don't have a maximum non-base, at this point we're fucked so just give back self
                foundId = context.getSelfId();
                return;
            }
        }

        currentNode = (ExternalPointer<A>) initialPointer;
        byte[] findIdData = findId.getValueAsByteArray();
        byte[] skipIdData = context.getSelfId().getValueAsByteArray();

        // move forward until you can't move forward anymore
        while (true) {
            Id oldCurrentNodeId = currentNode.getId();

            GetClosestFingerResponse<A> gcpfr = sendRequestAndWait(new GetClosestFingerRequest(findIdData, skipIdData),
                    currentNode.getAddress(), GetClosestFingerResponse.class, Duration.ofSeconds(3L));

            A address = gcpfr.getAddress();
            byte[] respIdData = gcpfr.getId();
            Id id = new Id(respIdData, currentNode.getId().getLimitAsByteArray());

            if (address == null) {
                currentNode = new ExternalPointer<>(id, currentNode.getAddress());
            } else {
                currentNode = new ExternalPointer<>(id, address);
            }

            if (id.equals(oldCurrentNodeId) || id.equals(context.getSelfId())) {
                break;
            }
        }

        foundId = currentNode.getId();
        if (!currentNode.getId().equals(context.getSelfId())) {
            foundAddress = currentNode.getAddress();
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
