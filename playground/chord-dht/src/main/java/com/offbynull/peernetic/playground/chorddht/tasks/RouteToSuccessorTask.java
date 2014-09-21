package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.javaflow.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse.ExternalSuccessorEntry;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse.InternalSuccessorEntry;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse.SuccessorEntry;
import com.offbynull.peernetic.playground.chorddht.shared.ChordUtils;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class RouteToSuccessorTask<A> extends SimpleJavaflowTask<A, byte[]> {
    
    private final ChordContext<A> context;
    
    private final Id findId;
    
    private Id foundId;
    private A foundAddress;
    
    
    public static <A> RouteToSuccessorTask<A> create(Instant time, ChordContext<A> context, Id findId) throws Exception {
        // create
        RouteToSuccessorTask<A> task = new RouteToSuccessorTask<>(context, findId);
        JavaflowActor actor = new JavaflowActor(task);
        task.initialize(time, actor);

        return task;
    }
    
    private RouteToSuccessorTask(ChordContext<A> context, Id findId) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        
        Validate.notNull(context);
        Validate.notNull(findId);
        this.context = context;
        this.findId = findId;
    }
    
    @Override
    public void execute() throws Exception {
        // find predecessor
        RouteToPredecessorTask<A> routeToPredTask = RouteToPredecessorTask.create(getTime(), context, findId);
        getFlowControl().waitUntilFinished(routeToPredTask.getActor(), Duration.ofSeconds(1L));
        Pointer foundPred = routeToPredTask.getResult();

        if (foundPred == null) {
            return;
        }

        if (foundPred instanceof InternalPointer) {
            Pointer successor = context.getFingerTable().get(0);

            if (successor instanceof InternalPointer) {
                foundId = successor.getId(); // id will always be the same as us
            } else if (successor instanceof ExternalPointer) {
                ExternalPointer<A> externalSuccessor = (ExternalPointer<A>) successor;
                foundId = externalSuccessor.getId();
                foundAddress = externalSuccessor.getAddress();
            } else {
                throw new IllegalStateException();
            }
        } else if (foundPred instanceof ExternalPointer) {
            ExternalPointer<A> externalPred = (ExternalPointer<A>) foundPred;

            GetSuccessorResponse<A> gsr = getFlowControl().sendRequestAndWait(new GetSuccessorRequest(), externalPred.getAddress(),
                    GetSuccessorResponse.class, Duration.ofSeconds(3L));

            SuccessorEntry successorEntry = gsr.getEntries().get(0);

            A senderAddress = context.getEndpointIdentifier().identify(getSource());
            A address;
            if (successorEntry instanceof InternalSuccessorEntry) { // this means the successor to the node is itself
                address = senderAddress;
            } else if (successorEntry instanceof ExternalSuccessorEntry) {
                address = ((ExternalSuccessorEntry<A>) successorEntry).getAddress();
            } else {
                throw new IllegalStateException();
            }

            // ask for that successor's id, wait for response here
            GetIdResponse gir = getFlowControl().sendRequestAndWait(new GetIdRequest(), address, GetIdResponse.class,
                    Duration.ofSeconds(3L));

            int bitSize = ChordUtils.getBitLength(findId);
            foundId = new Id(gir.getId(), bitSize);
            foundAddress = context.getEndpointIdentifier().identify(getSource());
        } else {
            throw new IllegalStateException();
        }
    }

    public Pointer getResult() {
        if (foundId == null) {
            return null;
        }
        
        if (foundId.equals(context.getSelfId())) {
            return new InternalPointer(foundId);
        }
        
        return new ExternalPointer<>(foundId, foundAddress);
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
