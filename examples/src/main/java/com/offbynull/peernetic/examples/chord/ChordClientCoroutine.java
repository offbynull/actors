package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.examples.chord.internalmessages.Start;
import com.offbynull.peernetic.examples.common.coroutines.ParentCoroutine;

public final class ChordClientCoroutine implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        Start start = ctx.getIncomingMessage();
        String timerPrefix = start.getTimerPrefix();
        
        State state = new State(timerPrefix);
        ParentCoroutine parentCoroutine = new ParentCoroutine("", timerPrefix, ctx);
        
        // JOIN (or just initialize if no bootstrap node is set)
        parentCoroutine.add("join", new JoinTask("join", state));
        parentCoroutine.run(cnt); // run until all added coroutines are finished and removed
        
        // RUN
        parentCoroutine.add("updateothers", new UpdateOthersTask("updateothers", state)); // notify our fingers that we're here (finite)
        parentCoroutine.add("fixfinger", new FixFingerTableTask("fixfinger", state));
        parentCoroutine.add("stabilize", new StabilizeTask("fixfinger", state));
        parentCoroutine.add("checkpred", new CheckPredecessorTask("checkpred", state));
        parentCoroutine.add("responder", new ResponderTask("responder", state));
        parentCoroutine.run(cnt); // run until all added coroutines are finished and removed
    }
    
}
