package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import org.apache.commons.lang3.Validate;

// a subcoroutine that wraps a normal coroutine
public final class CoroutineSubcoroutine implements Subcoroutine<Void> {

    private final String sourceId;
    private final Coroutine coroutine;

    public CoroutineSubcoroutine(String sourceId, Coroutine coroutine) {
        Validate.notNull(sourceId);
        Validate.notNull(coroutine);
        this.sourceId = sourceId;
        this.coroutine = coroutine;
    }
    
    @Override
    public String getSourceId() {
        return sourceId;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        coroutine.run(cnt);
        return null;
    }
}
