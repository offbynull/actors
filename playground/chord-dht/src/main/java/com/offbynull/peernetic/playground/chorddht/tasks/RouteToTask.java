package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.javaflow.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestFingerResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ChordHelper;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class RouteToTask<A> extends SimpleJavaflowTask<A, byte[]> {
    
    private final ChordContext<A> context;
    
    private final Id findId;
    private ExternalPointer<A> currentNode;
    
    private Id foundId;
    private A foundAddress;
    
    private final ChordHelper<A, byte[]> chordHelper;
    
    
    public static <A> RouteToTask<A> create(Instant time, ChordContext<A> context, Id findId) throws Exception {
        // create
        RouteToTask<A> task = new RouteToTask<>(context, findId);
        JavaflowActor actor = new JavaflowActor(task);
        task.initialize(time, actor);

        return task;
    }
    
    private RouteToTask(ChordContext<A> context, Id findId) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        
        Validate.notNull(context);
        Validate.notNull(findId);
        this.context = context;
        this.findId = findId;
        this.chordHelper = new ChordHelper<>(getState(), getFlowControl(), context);
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
        Id skipId = context.getSelfId();

        // move forward until you can't move forward anymore
        while (true) {
            Id oldCurrentNodeId = currentNode.getId();

            GetClosestFingerResponse<A> gcpfr = chordHelper.sendGetClosestFingerRequest(currentNode.getAddress(), findId, skipId);
            if (gcpfr == null) {
                return;
            }

            A address = gcpfr.getAddress();
            Id id = chordHelper.toId(gcpfr.getId());

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

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
