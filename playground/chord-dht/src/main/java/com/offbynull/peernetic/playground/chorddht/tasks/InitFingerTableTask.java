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
import com.offbynull.peernetic.playground.chorddht.messages.external.NotifyRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.NotifyResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ChordUtils;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.FingerTable;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import com.offbynull.peernetic.playground.chorddht.shared.SuccessorTable;
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

        // send priming message
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
    public void run() {
        try {
            int maxIdx = ChordUtils.getBitLength(context.getSelfId());
            
            // start timer
            scheduleTimer();
            
            
            // create fingertable -- find our successor from the bootstrap node and store it in the fingertable
            FingerTable fingerTable = new FingerTable(new InternalPointer(context.getSelfId()));
            Id expectedSuccesorId = fingerTable.getExpectedId(0);
            
            FindSuccessorResponse<A> fsr = sendAndWaitUntilResponse(new FindSuccessorRequest(expectedSuccesorId.getValueAsByteArray()),
                    bootstrapNode.getAddress(), FindSuccessorResponse.class);
            
            if (fsr.getId() == null) {
                throw new IllegalStateException();
            }
            
            ExternalPointer<A> successor;
            if (fsr.getAddress() == null) {
                successor = new ExternalPointer<>(new Id(fsr.getId(), context.getSelfId().getLimitAsByteArray()),
                        bootstrapNode.getAddress());
            } else {
                successor = new ExternalPointer<>(new Id(fsr.getId(), context.getSelfId().getLimitAsByteArray()), fsr.getAddress());
            }
            fingerTable.put(successor);
            
            
            // get our successor's pred 
            GetPredecessorResponse<A> gpr = sendAndWaitUntilResponse(new GetPredecessorRequest(), successor.getAddress(),
                    GetPredecessorResponse.class);

            if (gpr.getId() == null || gpr.getAddress() == null) {
                throw new IllegalStateException();
            }

            
            // populate fingertable
            for (int i = 0; i < maxIdx; i++) {
                if (fingerTable.get(i) instanceof ExternalPointer) {
                    continue;
                }
                
                // get expected id of entry in finger table
                Id findId = context.getFingerTable().getExpectedId(i);

                // route to id
                RouteToSuccessorTask<A> routeToNextFingerTask = RouteToSuccessorTask.createAndAssignToRouter(getTime(), context, findId);
                waitUntilFinished(routeToNextFingerTask.getEncapsulatingActor());
                Pointer foundFinger = routeToNextFingerTask.getResult();

                // set in to finger table
                if (foundFinger == null) {
                    continue;
                }

                if (!(foundFinger instanceof ExternalPointer)) {
                    throw new IllegalStateException();
                }

                context.getFingerTable().put((ExternalPointer<A>) foundFinger);
            }
            
            
            // create successor table and sync to finger table
            SuccessorTable<A> successorTable = new SuccessorTable<>(new InternalPointer(context.getSelfId()));
            successorTable.updateTrim(fingerTable.get(0));
            
            
            // notify our successor that we're its pred
            sendAndWaitUntilResponse(new NotifyRequest(context.getSelfId().getValueAsByteArray()),
                    successor.getAddress(), NotifyResponse.class);
            
            
            // update context fields
            context.setFingerTable(fingerTable);
            context.setSuccessorTable(successorTable);
            context.setPredecessor(new ExternalPointer<>(new Id(gpr.getId(), context.getSelfId().getLimitAsByteArray()), gpr.getAddress()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            unassignFromRouter(context, this);
        }
    }
    
    private static final class InternalStart {
        
    }
}
