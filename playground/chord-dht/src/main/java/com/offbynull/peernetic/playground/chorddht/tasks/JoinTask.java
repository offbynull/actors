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
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;

public final class JoinTask<A> extends BaseContinuableTask<A, byte[]> {

    private final ChordContext<A> context;

    public static <A> JoinTask<A> createAndAssignToRouter(Instant time, ChordContext<A> context) throws Exception {
        // create
        JoinTask<A> task = new JoinTask<>(context);
        ContinuationActor encapsulatingActor = new ContinuationActor(task);
        task.setEncapsulatingActor(encapsulatingActor);

        // register types here
        
        // sendRequest priming message
        encapsulatingActor.onStep(time, NullEndpoint.INSTANCE, new InternalStart());
        
        return task;
    }

    public static <A> void unassignFromRouter(ChordContext<A> context, JoinTask<A> task) {
        context.getRouter().removeActor(task.getEncapsulatingActor());
    }

    private JoinTask(ChordContext<A> context) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        this.context = context;
    }

    @Override
    public void execute() throws Exception {
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

        // ask for bootstrap node's id
        GetIdResponse gir = sendRequestAndWait(new GetIdRequest(), initialAddress, GetIdResponse.class, Duration.ofSeconds(3L));
        if (context.getSelfId().getValueAsBigInteger().equals(BigInteger.ONE)) {
            System.out.println("hit");
        }
        Id initialId = new Id(gir.getId(), maxIdx);

        // init finger table, successor table, etc...
        InitFingerTableTask<A> initFingerTableTask = InitFingerTableTask.createAndAssignToRouter(getTime(), context,
                new ExternalPointer<>(initialId, initialAddress));
        waitUntilFinished(initFingerTableTask.getEncapsulatingActor(), Duration.ofSeconds(1L));

        // notify our fingers that we're here, we don't need to wait until finished
        UpdateOthersTask<A> updateOthersTask = UpdateOthersTask.createAndAssignToRouter(getTime(), context);
        //waitUntilFinished(updateOthersTask.getEncapsulatingActor());
    }
    
    private static final class InternalStart {
        
    }
}
