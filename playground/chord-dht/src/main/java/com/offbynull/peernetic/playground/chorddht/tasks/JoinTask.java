package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.javaflow.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.FingerTable;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.ChordHelper;
import com.offbynull.peernetic.playground.chorddht.shared.SuccessorTable;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class JoinTask<A> extends SimpleJavaflowTask<A, byte[]> {

    private final ChordContext<A> context;
    private final ChordHelper<A, byte[]> chordHelper;

    public static <A> JoinTask<A> create(Instant time, ChordContext<A> context) throws Exception {
        // create
        JoinTask<A> task = new JoinTask<>(context);
        JavaflowActor actor = new JavaflowActor(task);
        task.initialize(time, actor);

        return task;
    }

    private JoinTask(ChordContext<A> context) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        
        Validate.notNull(context);
        this.context = context;
        this.chordHelper = new ChordHelper<>(getState(), getFlowControl(), context);
    }

    @Override
    public void execute() throws Exception {
        // initialize state
        A initialAddress = context.getBootstrapAddress();
        FingerTable<A> fingerTable = new FingerTable<>(new InternalPointer(context.getSelfId()));
        SuccessorTable<A> successorTable = new SuccessorTable<>(new InternalPointer(context.getSelfId()));

        // if no bootstrap address, we're the originator node, so initial successortable+fingertable is what we want.
        if (initialAddress == null) {
            context.setFingerTable(fingerTable);
            context.setSuccessorTable(successorTable);
            return;
        }

        // ask for bootstrap node's id
          // retry this req lots of time if it fails -- this is an important step, because it's required for the init finger table task to
          // work properly. it's okay if the init finger table task only resolves some fingers, but it won't be able to resolve any if we
          // don't have our bootstrap's id.
        GetIdResponse gir = chordHelper.sendGetIdRequest(initialAddress);
        Validate.validState(gir != null, "Joining failed -- bootstrap node did not respond to id request");
        Id initialId = chordHelper.convertToId(gir.getId());

        // init finger table, successor table, etc...
        InitFingerTableTask<A> initFingerTableTask = InitFingerTableTask.create(getTime(), context,
                new ExternalPointer<>(initialId, initialAddress));
        getFlowControl().waitUntilFinished(initFingerTableTask.getActor(), Duration.ofSeconds(1L));

        // notify our fingers that we're here, we don't need to wait until finished
        UpdateOthersTask<A> updateOthersTask = UpdateOthersTask.create(getTime(), context);
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
