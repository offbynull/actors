package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.BaseContinuableTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.ContinuationActor;
import com.offbynull.peernetic.playground.chorddht.messages.external.FindSuccessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.FindSuccessorResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetPredecessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetPredecessorResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ChordUtils;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.FingerTable;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.SuccessorTable;
import java.time.Duration;
import java.time.Instant;

public final class InitFingerTableTask<A> extends BaseContinuableTask<A, byte[]> {

    private final ChordContext<A> context;
    private final ExternalPointer<A> bootstrapNode;

    public static <A> InitFingerTableTask<A> createAndAssignToRouter(Instant time, ChordContext<A> context,
            ExternalPointer<A> bootstrapNode) throws Exception {
        // create
        InitFingerTableTask<A> task = new InitFingerTableTask<>(context, bootstrapNode);
        ContinuationActor encapsulatingActor = new ContinuationActor(task);
        task.setEncapsulatingActor(encapsulatingActor);

        // register types here

        // sendRequest priming message
        encapsulatingActor.onStep(time, NullEndpoint.INSTANCE, new InternalStart());
        
        return task;
    }

    public static <A> void unassignFromRouter(ChordContext<A> context, InitFingerTableTask<A> task) {
        context.getRouter().removeActor(task.getEncapsulatingActor());
    }

    private InitFingerTableTask(ChordContext<A> context, ExternalPointer<A> bootstrapNode) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        this.context = context;
        this.bootstrapNode = bootstrapNode;
    }

    @Override
    public void execute() throws Exception {
        int maxIdx = ChordUtils.getBitLength(context.getSelfId());

        // create fingertable -- find our successor from the bootstrap node and store it in the fingertable
        FingerTable fingerTable = new FingerTable(new InternalPointer(context.getSelfId()));
        Id expectedSuccesorId = fingerTable.getExpectedId(0);

        FindSuccessorResponse<A> fsr = sendRequestAndWait(new FindSuccessorRequest(expectedSuccesorId.getValueAsByteArray()),
                bootstrapNode.getAddress(), FindSuccessorResponse.class, Duration.ofSeconds(3L));
        ExternalPointer<A> successor = convertToExternalPointer(fsr.getId(), fsr.getAddress());
        if (successor == null) {
            throw new IllegalStateException();
        }
        fingerTable.put(successor);

        // get our successor's pred 
        GetPredecessorResponse<A> gpr = sendRequestAndWait(new GetPredecessorRequest(), successor.getAddress(),
                GetPredecessorResponse.class, Duration.ofSeconds(3L));

        // populate fingertable
        for (int i = 1; i < maxIdx; i++) {
            if (fingerTable.get(i) instanceof ExternalPointer) {
                continue;
            }

            // get expected id of entry in finger table
            Id findId = fingerTable.getExpectedId(i);

            // route to id
            fsr = sendRequestAndWait(new FindSuccessorRequest(findId.getValueAsByteArray()),
                    bootstrapNode.getAddress(), FindSuccessorResponse.class, Duration.ofSeconds(3L));
            ExternalPointer<A> foundFinger = convertToExternalPointer(fsr.getId(), fsr.getAddress());

            // set in to finger table
            if (foundFinger == null) {
                continue;
            }

            fingerTable.put((ExternalPointer<A>) foundFinger);
        }

        // create successor table and sync to finger table
        SuccessorTable<A> successorTable = new SuccessorTable<>(new InternalPointer(context.getSelfId()));
        successorTable.updateTrim(fingerTable.get(0));

//            // notify our successor that we're its pred
//            sendRequestAndWait(new NotifyRequest(context.getSelfId().getValueAsByteArray()),
//                    successor.getAddress(), NotifyResponse.class);
        // update context fields
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
        if (idData == null) {
            throw new IllegalStateException();
        }

        if (address == null) {
            return new ExternalPointer<>(new Id(idData, context.getSelfId().getLimitAsByteArray()), bootstrapNode.getAddress());
        } else {
            return new ExternalPointer<>(new Id(idData, context.getSelfId().getLimitAsByteArray()), address);
        }
    }
    
    private static final class InternalStart {
        
    }
}
