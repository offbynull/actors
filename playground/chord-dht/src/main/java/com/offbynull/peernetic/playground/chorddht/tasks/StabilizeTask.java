package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.javaflow.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetPredecessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetPredecessorResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse.ExternalSuccessorEntry;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse.InternalSuccessorEntry;
import com.offbynull.peernetic.playground.chorddht.messages.external.NotifyRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.NotifyResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ChordUtils;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class StabilizeTask<A> extends SimpleJavaflowTask<A, byte[]> {
    
    private final ChordContext<A> context;

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
        this.context = context;
    }

    @Override
    public void execute() throws Exception {
        while (true) {
            Pointer existingSuccessor = context.getFingerTable().get(0);
            if (existingSuccessor.getId().equals(context.getSelfId())) {
                return;
            }

            // ask for successor's pred
            A successorAddress = ((ExternalPointer<A>) existingSuccessor).getAddress();
            GetPredecessorResponse<A> gpr = getFlowControl().sendRequestAndWait(new GetPredecessorRequest(), successorAddress,
                    GetPredecessorResponse.class, Duration.ofSeconds(3L));

            // check to see if predecessor is between us and our successor
            if (gpr.getId() == null) {
                return;
            }

            A address = gpr.getAddress();
            byte[] idData = gpr.getId();

            Id potentiallyNewSuccessorId = new Id(idData, context.getSelfId().getLimitAsByteArray());
            Id existingSuccessorId = ((ExternalPointer<A>) existingSuccessor).getId();

            if (!potentiallyNewSuccessorId.isWithin(context.getSelfId(), false, existingSuccessorId, false)) {
                return;
            }

            // it is between us and our successor, so notify it
            ExternalPointer<A> newSuccessor = new ExternalPointer<>(potentiallyNewSuccessorId, address);

            getFlowControl().sendRequestAndWait(new NotifyRequest(context.getSelfId().getValueAsByteArray()),
                    ((ExternalPointer<A>) newSuccessor).getAddress(),
                    NotifyResponse.class, Duration.ofSeconds(3L));

            // ask new successor for its successors
            GetSuccessorResponse<A> gsr = getFlowControl().sendRequestAndWait(new GetSuccessorRequest(),
                    ((ExternalPointer<A>) newSuccessor).getAddress(),
                    GetSuccessorResponse.class, Duration.ofSeconds(3L));

            int bitSize = ChordUtils.getBitLength(context.getSelfId());
            List<Pointer> subsequentSuccessors = new ArrayList<>();
            gsr.getEntries().stream().map(x -> {
                Id id = new Id(x.getId(), bitSize);

                if (x instanceof InternalSuccessorEntry) {
                    return new InternalPointer(id);
                } else if (x instanceof ExternalSuccessorEntry) {
                    return new ExternalPointer<>(id, ((ExternalSuccessorEntry<A>) x).getAddress());
                } else {
                    throw new IllegalStateException();
                }
            }).forEachOrdered(x -> subsequentSuccessors.add(x));

            // mark it as our new successor
            context.getSuccessorTable().update(newSuccessor, subsequentSuccessors);
            context.getFingerTable().put(newSuccessor);
        }
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
