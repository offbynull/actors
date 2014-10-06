package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.javaflow.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ChordHelper;
import com.offbynull.peernetic.playground.chorddht.model.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.model.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.model.Pointer;
import com.offbynull.peernetic.playground.chorddht.shared.ChordOperationException;
import java.time.Instant;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RouteToPredecessorTask<A> extends SimpleJavaflowTask<A, byte[]> {
    
    private static final Logger LOG = LoggerFactory.getLogger(RouteToPredecessorTask.class);

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
        this.findId = findId;
        this.chordHelper = new ChordHelper<>(getState(), getFlowControl(), context);
    }
    
    @Override
    public void execute() throws Exception {
        Id selfId = chordHelper.getSelfId();
        
        LOG.debug("Routing to predecessor of {}", findId);
        
        try {
            if (findId.isWithin(selfId, false, chordHelper.getSuccessor().getId(), true)) {
                LOG.debug("Predecessor is between self and successor. Stopping.");
                foundId = selfId;
                foundAddress = null;
                return;
            }

            Pointer initialPointer = chordHelper.getClosestPrecedingFinger(findId);
            if (initialPointer instanceof InternalPointer) { // if the closest predecessor is yourself -- which means the ft is empty
                LOG.debug("Self is the closest preceding finger. Stopping.");
                foundId = selfId;
                foundAddress = null;
                return;
            } else if (initialPointer instanceof ExternalPointer) {
                currentNode = (ExternalPointer<A>) initialPointer;
                LOG.debug("{} is the closest preceding finger on file.", currentNode);
            } else {
                throw new IllegalStateException();
            }

            // move forward until you can't move forward anymore
            while (true) {
                LOG.debug("Querying successor of {}.", currentNode.getId());
                GetSuccessorResponse<A> gsr = chordHelper.sendGetSuccessorRequest(currentNode.getAddress());
                Id succId = chordHelper.toId(gsr.getEntries().get(0).getId());
                
                LOG.debug("Successor of {} is {}.", currentNode.getId(), succId);
                if (findId.isWithin(currentNode.getId(), false, succId, true)) {
                    LOG.debug("{} is between {} and {}.", findId, currentNode.getId(), succId);
                    break;
                }

                GetClosestPrecedingFingerResponse<A> gcpfr = chordHelper.sendGetClosestPrecedingFingerRequest(currentNode.getAddress(), findId);
                A address = gcpfr.getAddress();
                Id id = chordHelper.toId(gcpfr.getId());

                if (address == null) {
                    currentNode = new ExternalPointer<>(id, currentNode.getAddress());
                } else {
                    currentNode = new ExternalPointer<>(id, address);
                }
                
                LOG.debug("{} reports its closest predecessor is {}.", succId, id);
            }

            foundId = currentNode.getId();
            if (!currentNode.getId().equals(selfId)) {
                foundAddress = currentNode.getAddress();
            }
            
            LOG.debug("Found {} at {}.", foundId, foundAddress);
        } catch (ChordOperationException coe) {
            LOG.error(coe.getMessage());
        }
    }

    public Pointer getResult() {
        if (foundId == null) {
            return null;
        }
        
        if (chordHelper.isSelfId(foundId)) {
            return new InternalPointer(foundId);
        }
        
        return new ExternalPointer<>(foundId, foundAddress);
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
