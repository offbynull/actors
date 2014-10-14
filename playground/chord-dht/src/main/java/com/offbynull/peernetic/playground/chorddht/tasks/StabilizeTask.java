package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.skeleton.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetPredecessorResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse.ExternalSuccessorEntry;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse.InternalSuccessorEntry;
import com.offbynull.peernetic.playground.chorddht.shared.ChordHelper;
import com.offbynull.peernetic.playground.chorddht.model.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.model.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.model.Pointer;
import com.offbynull.peernetic.playground.chorddht.shared.ChordOperationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StabilizeTask<A> extends SimpleJavaflowTask<A, byte[]> {
    
    private static final Logger LOG = LoggerFactory.getLogger(StabilizeTask.class);
    
    private final ChordHelper<A, byte[]> chordHelper;

    public static <A> StabilizeTask<A> create(Instant time, ChordContext<A> context) throws Exception {
        // create
        StabilizeTask<A> task = new StabilizeTask<>(context);
        JavaflowActor actor = new JavaflowActor(task);
        task.initialize(time, actor);

        return task;
    }

    private StabilizeTask(ChordContext<A> context) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        
        Validate.notNull(context);
        this.chordHelper = new ChordHelper<>(getState(), getFlowControl(), context);
    }

    @Override
    public void execute() throws Exception {
        Id selfId = chordHelper.getSelfId();
        while (true) {
            chordHelper.sleep(1L);
            
            try {
                Pointer successor = chordHelper.getSuccessor();
                if (chordHelper.isSelfId(successor.getId())) {
                    continue;
                }

                // ask for successor's pred
                A successorAddress = ((ExternalPointer<A>) successor).getAddress();
                GetPredecessorResponse<A> gpr = chordHelper.sendGetPredecessorRequest(successorAddress);

                // check to see if predecessor is between us and our successor
                if (gpr.getId() != null) {
                    A address = gpr.getAddress();
                    Id potentiallyNewSuccessorId = chordHelper.toId(gpr.getId());
                    Id existingSuccessorId = ((ExternalPointer<A>) successor).getId();

                    if (potentiallyNewSuccessorId.isWithin(selfId, false, existingSuccessorId, false)) {
                        // it is between us and our successor, so set it and notify it that we are its predecessor
                        ExternalPointer<A> newSuccessor = new ExternalPointer<>(potentiallyNewSuccessorId, address);
                        chordHelper.setSuccessor(newSuccessor);
                        chordHelper.sendNotifyRequest(((ExternalPointer<A>) newSuccessor).getAddress(), selfId);
                        
                        successor = newSuccessor;
                        successorAddress = ((ExternalPointer<A>) newSuccessor).getAddress();
                    }
                }

                // successor may have been updated by block above
                // ask successor for its successors
                GetSuccessorResponse<A> gsr = chordHelper.sendGetSuccessorRequest(successorAddress);

                List<Pointer> subsequentSuccessors = new ArrayList<>();
                gsr.getEntries().stream().map(x -> {
                    Id id = chordHelper.toId(x.getId());

                    if (x instanceof InternalSuccessorEntry) {
                        return new InternalPointer(id);
                    } else if (x instanceof ExternalSuccessorEntry) {
                        return new ExternalPointer<>(id, ((ExternalSuccessorEntry<A>) x).getAddress());
                    } else {
                        throw new IllegalStateException();
                    }
                }).forEachOrdered(x -> subsequentSuccessors.add(x));

                // mark it as our new successor
                chordHelper.updateSuccessor((ExternalPointer<A>) successor, subsequentSuccessors);
            } catch (ChordOperationException coe) {
                LOG.warn("Failed to stabilize");
            }
        }
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
