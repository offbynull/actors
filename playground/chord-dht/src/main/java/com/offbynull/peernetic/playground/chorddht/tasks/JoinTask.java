package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
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

public final class JoinTask<A> extends ChordTask<A> {


    public static <A> JoinTask<A> create(Instant time, ChordContext<A> context) throws Exception {
        // create
        JoinTask<A> task = new JoinTask<>(context);
        JavaflowActor actor = new JavaflowActor(task);
        task.initialize(time, actor);

        return task;
    }

    private JoinTask(ChordContext<A> context) {
        super(context);
    }

    @Override
    public void execute() throws Exception {
        // initialize state
        A initialAddress = getContext().getBootstrapAddress();
        FingerTable<A> fingerTable = new FingerTable<>(new InternalPointer(getContext().getSelfId()));
        SuccessorTable<A> successorTable = new SuccessorTable<>(new InternalPointer(getContext().getSelfId()));
        int maxIdx = ChordUtils.getBitLength(getContext().getSelfId());

        // if no bootstrap address, we're the originator node, so initial successortable+fingertable is what we want.
        if (initialAddress == null) {
            getContext().setFingerTable(fingerTable);
            getContext().setSuccessorTable(successorTable);
            return;
        }

        // ask for bootstrap node's id
        GetIdResponse gir = getFlowControl().sendRequestAndWait(new GetIdRequest(), initialAddress, GetIdResponse.class, Duration.ofSeconds(3L));
        if (getContext().getSelfId().getValueAsBigInteger().equals(BigInteger.ONE)) {
            System.out.println("hit");
        }
        Id initialId = new Id(gir.getId(), maxIdx);

        // init finger table, successor table, etc...
        InitFingerTableTask<A> initFingerTableTask = InitFingerTableTask.create(getTime(), getContext(),
                new ExternalPointer<>(initialId, initialAddress));
        getFlowControl().waitUntilFinished(initFingerTableTask.getActor(), Duration.ofSeconds(1L));

        // notify our fingers that we're here, we don't need to wait until finished
        UpdateOthersTask<A> updateOthersTask = UpdateOthersTask.create(getTime(), getContext());
        //waitUntilFinished(updateOthersTask.getEncapsulatingActor());
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
    
    private static final class InternalStart {
        
    }
}
