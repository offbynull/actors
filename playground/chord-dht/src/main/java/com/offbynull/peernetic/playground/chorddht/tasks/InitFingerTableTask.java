package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.javaflow.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.FindSuccessorResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetPredecessorResponse;
import com.offbynull.peernetic.playground.chorddht.shared.IdUtils;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.FingerTable;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.ChordHelper;
import com.offbynull.peernetic.playground.chorddht.shared.ChordOperationException;
import com.offbynull.peernetic.playground.chorddht.shared.SuccessorTable;
import java.time.Instant;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InitFingerTableTask<A> extends SimpleJavaflowTask<A, byte[]> {

    private static final Logger LOG = LoggerFactory.getLogger(InitFingerTableTask.class);
    
    private final ChordContext<A> context;
    private final ExternalPointer<A> bootstrapNode;
    private final ChordHelper<A, byte[]> chordHelper;

    public static <A> InitFingerTableTask<A> create(Instant time, ChordContext<A> context,
            ExternalPointer<A> bootstrapNode) throws Exception {
        // create
        InitFingerTableTask<A> task = new InitFingerTableTask<>(context, bootstrapNode);
        JavaflowActor actor = new JavaflowActor(task);
        task.initialize(time, actor);

        return task;
    }

    private InitFingerTableTask(ChordContext<A> context, ExternalPointer<A> bootstrapNode) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        
        Validate.notNull(context);
        Validate.notNull(bootstrapNode);
        this.context = context;
        this.bootstrapNode = bootstrapNode;
        this.chordHelper = new ChordHelper<>(getState(), getFlowControl(), context);
    }

    @Override
    public void execute() throws Exception {
        int maxIdx = IdUtils.getBitLength(context.getSelfId());

        // create fingertable -- find our successor from the bootstrap node and store it in the fingertable
        FingerTable fingerTable = new FingerTable(new InternalPointer(context.getSelfId()));
        Id expectedSuccesorId = fingerTable.getExpectedId(0);

        FindSuccessorResponse<A> fsr = chordHelper.sendFindSuccessorRequest(bootstrapNode.getAddress(), expectedSuccesorId);
        chordHelper.failIfSelf(fsr);
        ExternalPointer<A> successor = chordHelper.toExternalPointer(fsr, bootstrapNode.getAddress());
        fingerTable.put(successor);

        // get our successor's pred 
        GetPredecessorResponse<A> gpr = chordHelper.sendGetPredecessorRequest(successor.getAddress());
        chordHelper.failIfSelf(gpr);

        // populate fingertable
        for (int i = 1; i < maxIdx; i++) {
            // successor may extend multiple slots in to the fingertable... if it does, skip this entry and go to the next
            if (fingerTable.get(i) instanceof ExternalPointer) {
                LOG.warn("No need to find finger for index {}", i);
                continue;
            }

            // get expected id of entry in finger table
            Id findId = fingerTable.getExpectedId(i);

            // route to id if possible... if failed to route, skip this entry and go to the next (this will repair eventually once this node
            // is up)
            try {
                fsr = chordHelper.sendFindSuccessorRequest(bootstrapNode.getAddress(), findId);
            } catch (ChordOperationException coe) {
                LOG.warn("Unable to find finger for index {}", i);
                continue;
            }

            // set in to finger table
            ExternalPointer<A> foundFinger = chordHelper.toExternalPointer(fsr, bootstrapNode.getAddress());
            fingerTable.put(foundFinger);
        }

        // create successor table and sync to finger table
        SuccessorTable<A> successorTable = new SuccessorTable<>(new InternalPointer(context.getSelfId()));
        successorTable.updateTrim(fingerTable.get(0));

        chordHelper.setTables(fingerTable, successorTable);
        chordHelper.setPredecessor(gpr);
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
