package com.offbynull.peernetic.actor;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import org.apache.commons.lang3.Validate;

public final class CoroutineActor implements Actor {
    private CoroutineRunner coroutineRunner;
    private boolean executing;

    public CoroutineActor(Coroutine task) {
        Validate.notNull(task);
        coroutineRunner = new CoroutineRunner(task);
        executing = true;
    }

    @Override
    public boolean onStep(Context context) throws Exception {
        // if continuation has ended, ignore any further messages
        if (executing) {
            coroutineRunner.setContext(context); // set once
            executing = coroutineRunner.execute();
        }
        
        return executing;
    }

    public boolean isFinished() {
        return executing;
    }
    
}
