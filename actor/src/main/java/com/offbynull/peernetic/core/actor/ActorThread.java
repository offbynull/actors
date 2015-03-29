package com.offbynull.peernetic.core.actor;

import com.offbynull.peernetic.core.Shuttle;
import org.apache.commons.lang3.Validate;

public final class ActorThread {
    private final Thread thread;
    private final ActorRunnable actorRunnable;

    ActorThread(Thread thread, ActorRunnable actorRunnable) {
        Validate.notNull(thread);
        Validate.notNull(actorRunnable);
        this.thread = thread;
        this.actorRunnable = actorRunnable;
    }

    public Thread getThread() {
        return thread;
    }

    public ActorRunnable getActorRunnable() {
        return actorRunnable;
    }

    public Shuttle getShuttle() {
        return actorRunnable.getShuttle();
    }
    
}
