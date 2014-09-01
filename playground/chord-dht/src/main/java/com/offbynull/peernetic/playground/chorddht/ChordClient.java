package com.offbynull.peernetic.playground.chorddht;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.common.message.ByteArrayNonceAccessor;
import com.offbynull.peernetic.common.message.ByteArrayNonceGenerator;
import com.offbynull.peernetic.common.transmission.Router;
import com.offbynull.peernetic.playground.chorddht.messages.internal.Start;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import com.offbynull.peernetic.playground.chorddht.tasks.CheckPredecessorTask;
import com.offbynull.peernetic.playground.chorddht.tasks.FixFingerTableTask;
import com.offbynull.peernetic.playground.chorddht.tasks.GenerateFingerTableTask;
import com.offbynull.peernetic.playground.chorddht.tasks.ResponderTask;
import com.offbynull.peernetic.playground.chorddht.tasks.StabilizeTask;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.javaflow.Continuation;

public final class ChordClient<A> implements ContinuableTask {

    private Instant time;
    private Endpoint source;
    private Object message;


    
    @Override
    public void run() {
        try {
            Start<A> start = (Start<A>) message;
            ChordContext<A> context = new ChordContext(start.getActiveListener(), start.getLinkListener(), start.getUnlinkListener());

            context.setEndpointDirectory(start.getEndpointDirectory());
            context.setEndpointIdentifier(start.getEndpointIdentifier());
            context.setEndpointScheduler(start.getEndpointScheduler());
            context.setSelfEndpoint(start.getSelfEndpoint());
            context.setSelfId(start.getSelfId());
            context.setBootstrapAddress(start.getBootstrapAddress());
            context.setNonceAccessor(new ByteArrayNonceAccessor());
            context.setNonceGenerator(new ByteArrayNonceGenerator(8));
            context.setRouter(new Router<>(
                    context.getSelfEndpoint(),
                    context.getNonceGenerator(),
                    context.getNonceAccessor(),
                    context.getEndpointDirectory()));
            
            GenerateFingerTableTask<A> gftt = GenerateFingerTableTask.createAndAssignToRouter(time, context);
            
            while(!gftt.getEncapsulatingActor().isFinished()) {
                Continuation.suspend();
                context.getRouter().routeMessage(time, message, source);
            }

            FixFingerTableTask<A> fftt = FixFingerTableTask.createAndAssignToRouter(time, context);
            StabilizeTask<A> st = StabilizeTask.createAndAssignToRouter(time, context);
            CheckPredecessorTask<A> cpt = CheckPredecessorTask.createAndAssignToRouter(time, context);
            ResponderTask<A> rt = ResponderTask.createAndAssignToRouter(time, context);
            
            while (true) {
                Continuation.suspend();
                context.getRouter().routeMessage(time, message, source);
                
                notifyStateChange(context);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    private Set<Pointer> lastNotifiedPointers = new HashSet<>();
    private void notifyStateChange(ChordContext<A> context) {
        Set<Pointer> newPointers = new HashSet<>(Arrays.<Pointer>asList(
                context.getFingerTable().dump().stream().filter(x -> x instanceof ExternalPointer).toArray(x -> new Pointer[x])));
        
        Set<Pointer> addedPointers = new HashSet<>(newPointers);
        addedPointers.removeAll(lastNotifiedPointers);
        addedPointers.forEach(x -> context.getLinkListener().linked(context.getSelfId(), x.getId()));

        Set<Pointer> removedPointers = new HashSet<>(lastNotifiedPointers);
        removedPointers.removeAll(newPointers);
        removedPointers.forEach(x -> context.getUnlinkListener().unlinked(context.getSelfId(), x.getId()));
        
        lastNotifiedPointers = newPointers;
    }

    @Override
    public void setMessage(Object message) {
        this.message = message;
    }

    @Override
    public void setSource(Endpoint source) {
        this.source = source;
    }

    @Override
    public void setTime(Instant time) {
        this.time = time;
    }
}
