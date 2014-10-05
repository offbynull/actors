package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.javaflow.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ChordHelper;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class RouteToPredecessorTask<A> extends SimpleJavaflowTask<A, byte[]> {
    
    private final ChordContext<A> context;
    
    private final Id findId;
    private ExternalPointer<A> currentNode;
    
    private Id foundId;
    private A foundAddress;
    
    private final ChordHelper<A, byte[]> chordHelper;
    
    
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
        this.chordHelper = new ChordHelper<>(getState(), getFlowControl(), context);
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

        // move forward until you can't move forward anymore
        while (!findId.isWithin(currentNode.getId(), false, querySuccessorId(currentNode), true)) {
            GetClosestPrecedingFingerResponse<A> gcpfr = chordHelper.sendGetClosestPrecedingFingerRequest(currentNode.getAddress(), findId);

            A address = gcpfr.getAddress();
            Id id = chordHelper.convertToId(gcpfr.getId());

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
        GetSuccessorResponse<A> gsr = chordHelper.sendGetSuccessorRequest(pointer.getAddress());
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
