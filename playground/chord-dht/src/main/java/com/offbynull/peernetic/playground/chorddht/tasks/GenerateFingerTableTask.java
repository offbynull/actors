package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.BaseContinuableTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.ContinuationActor;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ChordUtils;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.FingerTable;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.SuccessorTable;
import java.time.Instant;
import org.apache.commons.javaflow.Continuation;

public final class GenerateFingerTableTask<A> extends BaseContinuableTask<A, byte[]> {

    private final ChordContext<A> context;

    public static <A> GenerateFingerTableTask<A> createAndAssignToRouter(Instant time, ChordContext<A> context) throws Exception {
        // create
        GenerateFingerTableTask<A> task = new GenerateFingerTableTask<>(context);
        ContinuationActor encapsulatingActor = new ContinuationActor(task);
        task.setEncapsulatingActor(encapsulatingActor);

        // register types here
        
        // send priming message
        encapsulatingActor.onStep(time, NullEndpoint.INSTANCE, new InternalStart());
        
        return task;
    }

    public static <A> void unassignFromRouter(ChordContext<A> context, GenerateFingerTableTask<A> task) {
        context.getRouter().removeActor(task.getEncapsulatingActor());
    }

    private GenerateFingerTableTask(ChordContext<A> context) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        this.context = context;
    }

    @Override
    public void run() {
        try {
            // initialize state
            A initialAddress = context.getBootstrapAddress();
            FingerTable<A> fingerTable = new FingerTable<>(new InternalPointer(context.getSelfId()));
            SuccessorTable<A> successorTable = new SuccessorTable<>(new InternalPointer(context.getSelfId()));
            int maxIdx = ChordUtils.getBitLength(context.getSelfId());
            
            // if no bootstrap address, we're the originator node, so initial successortable+fingertable is what we want.
            if (initialAddress == null) {
                context.setFingerTable(fingerTable);
                context.setSuccessorTable(successorTable);
                return;
            }

            // start timer
            scheduleTimer();

            // ask for bootstrap node's id
            GetIdResponse gir = sendAndWaitUntilResponse(new GetIdRequest(), initialAddress, GetIdResponse.class);
            Id initialId = new Id(gir.getId(), maxIdx);
            ExternalPointer<A> fromNode = new ExternalPointer<>(initialId, initialAddress);

            // fill up finger table
            for (int i = 0; i <= maxIdx; i++) {
                Id findId = fingerTable.getExpectedId(i);

                RouteToFingerTask<A> routeToFingerTask = RouteToFingerTask.createAndAssignToRouter(getTime(), context, fromNode, findId);
                waitUntilFinished(routeToFingerTask.getEncapsulatingActor());

                ExternalPointer<A> foundFinger = routeToFingerTask.getResult();
                fingerTable.put(foundFinger);
            }
            
            context.setFingerTable(fingerTable);
            context.setSuccessorTable(successorTable);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            unassignFromRouter(context, this);
        }
    }
    
    private static final class InternalStart {
        
    }
}
