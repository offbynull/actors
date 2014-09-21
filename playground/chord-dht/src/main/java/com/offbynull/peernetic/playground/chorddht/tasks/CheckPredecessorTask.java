package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import java.time.Duration;
import java.time.Instant;

public final class CheckPredecessorTask<A> extends ChordTask<A> {

    public static <A> CheckPredecessorTask<A> create(Instant time, ChordContext<A> context) throws Exception {
        CheckPredecessorTask<A> task = new CheckPredecessorTask<>(context);
        JavaflowActor actor = new JavaflowActor(task);
        task.initialize(time, actor);

        return task;
    }

    
    private CheckPredecessorTask(ChordContext<A> context) {
        super(context);
    }

    @Override
    public void execute() throws Exception {
        while (true) {
            ExternalPointer<A> predecessor = getContext().getPredecessor();

            if (predecessor == null) {
                getFlowControl().wait(Duration.ofSeconds(1L));
                continue;
            }
            GetIdResponse gir = getFlowControl().sendRequestAndWait(new GetIdRequest(), predecessor.getAddress(), GetIdResponse.class,
                    Duration.ofSeconds(3L));
            Id id = new Id(gir.getId(), getContext().getSelfId().getLimitAsByteArray());

            if (!id.equals(predecessor.getId())) {
                getContext().setPredecessor(null);
            }

            getFlowControl().wait(Duration.ofSeconds(1L));
        }
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
