package com.offbynull.peernetic.playground.chorddht;

import com.offbynull.peernetic.common.message.ByteArrayNonceAccessor;
import com.offbynull.peernetic.common.message.ByteArrayNonceGenerator;
import com.offbynull.peernetic.common.skeleton.Router;
import com.offbynull.peernetic.javaflow.BaseJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordActiveListener.Mode;
import com.offbynull.peernetic.playground.chorddht.ChordDeactiveListener.Type;
import com.offbynull.peernetic.playground.chorddht.messages.internal.Start;
import com.offbynull.peernetic.playground.chorddht.model.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.model.Pointer;
import com.offbynull.peernetic.playground.chorddht.tasks.CheckPredecessorTask;
import com.offbynull.peernetic.playground.chorddht.tasks.FixFingerTableTask;
import com.offbynull.peernetic.playground.chorddht.tasks.JoinTask;
import com.offbynull.peernetic.playground.chorddht.tasks.ResponderTask;
import com.offbynull.peernetic.playground.chorddht.tasks.StabilizeTask;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.javaflow.Continuation;

public final class ChordClient<A> extends BaseJavaflowTask {


    
    @Override
    public void run() {
        Start<A> start = (Start<A>) getMessage();
        ChordContext<A> context = new ChordContext(start.getActiveListener(), start.getLinkListener(), start.getUnlinkListener(),
                start.getDeactiveListener());

        try {
            context.getActiveListener().active(start.getSelfId(), start.getBootstrapAddress() == null ? Mode.SEED : Mode.JOIN);

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
            
            JoinTask<A> gftt = JoinTask.create(getTime(), context);
            
            while(!gftt.getActor().isFinished()) {
                Continuation.suspend();
                context.getRouter().routeMessage(getTime(), getMessage(), getSource());
            }

            FixFingerTableTask<A> fftt = FixFingerTableTask.create(getTime(), context);
            StabilizeTask<A> st = StabilizeTask.create(getTime(), context);
            CheckPredecessorTask<A> cpt = CheckPredecessorTask.create(getTime(), context);
            ResponderTask<A> rt = ResponderTask.create(getTime(), context);
            
            while (true) {
                Continuation.suspend();
                context.getRouter().routeMessage(getTime(), getMessage(), getSource());
                
                notifyStateChange(context);
            }
        } catch (Exception e) {
            context.getDeactiveListener().deactive(start.getSelfId(), Type.ERROR);
        }
    }
    
    private Set<Pointer> lastNotifiedPointers = new HashSet<>();
    private void notifyStateChange(ChordContext<A> context) {
        Set<Pointer> newPointers = new HashSet<>(Arrays.<Pointer>asList(
                context.getFingerTable().dump().stream().filter(x -> x instanceof ExternalPointer).toArray(x -> new Pointer[x])));
        if (context.getPredecessor() != null) {
            newPointers.add(context.getPredecessor());
        }
        
        Set<Pointer> addedPointers = new HashSet<>(newPointers);
        addedPointers.removeAll(lastNotifiedPointers);
        addedPointers.forEach(x -> context.getLinkListener().linked(context.getSelfId(), x.getId()));

        Set<Pointer> removedPointers = new HashSet<>(lastNotifiedPointers);
        removedPointers.removeAll(newPointers);
        removedPointers.forEach(x -> context.getUnlinkListener().unlinked(context.getSelfId(), x.getId()));
        
        lastNotifiedPointers = newPointers;
    }
}
