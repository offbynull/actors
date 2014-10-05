package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.javaflow.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.ChordHelper;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class CheckPredecessorTask<A> extends SimpleJavaflowTask<A, byte[]> {

    private final ChordContext<A> context;
    private final ChordHelper<A, byte[]> chordHelper;
    
    public static <A> CheckPredecessorTask<A> create(Instant time, ChordContext<A> context) throws Exception {
        CheckPredecessorTask<A> task = new CheckPredecessorTask<>(context);
        JavaflowActor actor = new JavaflowActor(task);
        task.initialize(time, actor);

        return task;
    }

    
    private CheckPredecessorTask(ChordContext<A> context) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        
        Validate.notNull(context);
        this.context = context;
        this.chordHelper = new ChordHelper<>(getState(), getFlowControl(), context);
    }

    @Override
    public void execute() throws Exception {
        while (true) {
            getFlowControl().wait(Duration.ofSeconds(1L));
                        
            ExternalPointer<A> predecessor = context.getPredecessor();
            if (predecessor == null) {
                // we don't have a predecessor to check
                continue;
            }
            
            // ask for our predecessor's current id
            GetIdResponse gir = chordHelper.sendGetIdRequest(predecessor.getAddress());
            
            if (gir == null) {
                // predecessor didn't respond -- clear our predecessor
                context.setPredecessor(null);
                continue;
            }
            
            Id id = chordHelper.convertToId(gir.getId());
            if (!id.equals(predecessor.getId())) {
                // predecessor responded with unexpected id -- clear our predecessor
                context.setPredecessor(null);
            }
        }
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
