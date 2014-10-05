package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.javaflow.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.FindSuccessorResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetPredecessorResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ChordUtils;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.FingerTable;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.ChordHelper;
import com.offbynull.peernetic.playground.chorddht.shared.SuccessorTable;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class InitFingerTableTask<A> extends SimpleJavaflowTask<A, byte[]> {

    private final ChordContext<A> context;
    private final ExternalPointer<A> bootstrapNode;
    private final ChordHelper<A, byte[]> networkHelper;

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
        this.networkHelper = new ChordHelper<>(getState(), getFlowControl(), context);
    }

    @Override
    public void execute() throws Exception {
        int maxIdx = ChordUtils.getBitLength(context.getSelfId());

        // create fingertable -- find our successor from the bootstrap node and store it in the fingertable
        FingerTable fingerTable = new FingerTable(new InternalPointer(context.getSelfId()));
        Id expectedSuccesorId = fingerTable.getExpectedId(0);

          // it's super important that we have our successor, as its the basis for the correctness of the chord ring. as such, we should
          // retry this alot of times before giving up.
        FindSuccessorResponse<A> fsr = networkHelper.sendFindSuccessorRequest(bootstrapNode.getAddress(), expectedSuccesorId);
        Validate.validState(fsr != null, "Unable to reach successor");
        ExternalPointer<A> successor = convertToExternalPointer(fsr.getId(), fsr.getAddress());
        fingerTable.put(successor);

        // get our successor's pred 
        GetPredecessorResponse<A> gpr = networkHelper.sendGetPredecessorRequest(successor.getAddress());
        Validate.validState(gpr != null, "Unable to get successor's predecessor");

        // populate fingertable
        for (int i = 1; i < maxIdx; i++) {
            if (fingerTable.get(i) instanceof ExternalPointer) {
                continue;
            }

            // get expected id of entry in finger table
            Id findId = fingerTable.getExpectedId(i);

            // route to id
            fsr = networkHelper.sendFindSuccessorRequest(bootstrapNode.getAddress(), findId);
            if (fsr == null) {
                continue;
            }

            // set in to finger table
            ExternalPointer<A> foundFinger = convertToExternalPointer(fsr.getId(), fsr.getAddress());
            fingerTable.put(foundFinger);
        }

        // create successor table and sync to finger table
        SuccessorTable<A> successorTable = new SuccessorTable<>(new InternalPointer(context.getSelfId()));
        successorTable.updateTrim(fingerTable.get(0));

//            // notify our successor that we're its pred
//            sendRequestAndWait(new NotifyRequest(context.getSelfId().getValueAsByteArray()),
//                    successor.getAddress(), NotifyResponse.class);
        // update getContext() fields
        context.setFingerTable(fingerTable);
        context.setSuccessorTable(successorTable);

        if (gpr.getId() != null) {
            Id gprId = new Id(gpr.getId(), context.getSelfId().getLimitAsByteArray());
            if (gprId.equals(context.getSelfId())) {
                context.setPredecessor(new ExternalPointer<>(gprId, gpr.getAddress()));
            }
        }
    }
    
    private ExternalPointer<A> convertToExternalPointer(byte[] idData, A address) {
        Validate.notNull(idData);

        if (address == null) {
            return new ExternalPointer<>(new Id(idData, context.getSelfId().getLimitAsByteArray()), bootstrapNode.getAddress());
        } else {
            return new ExternalPointer<>(new Id(idData, context.getSelfId().getLimitAsByteArray()), address);
        }
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
