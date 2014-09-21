package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.javaflow.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class RouteToPredecessorTask<A> extends SimpleJavaflowTask<A, byte[]> {
    
    private final ChordContext<A> context;
    
    private final Id findId;
    private ExternalPointer<A> currentNode;
    
    private Id foundId;
    private A foundAddress;
    
    
    public static <A> RouteToPredecessorTask<A> create(Instant time, ChordContext<A> context, Id findId) throws Exception {
        // create
        RouteToPredecessorTask<A> task = new RouteToPredecessorTask<>(context, findId);
        JavaflowActor actor = new JavaflowActor(task);
        task.initialize(time, actor);

        return task;
    }
    
    private RouteToPredecessorTask(ChordContext<A> context, Id findId) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        
        Validate.notNull(context);
        Validate.notNull(findId);
        this.context = context;
        this.findId = findId;
    }
    
    @Override
    public void execute() throws Exception {
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
            GetClosestPrecedingFingerResponse<A> gcpfr = getFlowControl().sendRequestAndWait(new GetClosestPrecedingFingerRequest(findIdData),
                    currentNode.getAddress(), GetClosestPrecedingFingerResponse.class, Duration.ofSeconds(3L));

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
    }
    
    private Id querySuccessorId(ExternalPointer<A> pointer) {
        GetSuccessorResponse<A> gsr = getFlowControl().sendRequestAndWait(new GetSuccessorRequest(),
                pointer.getAddress(), GetSuccessorResponse.class, Duration.ofSeconds(3L));
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

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
