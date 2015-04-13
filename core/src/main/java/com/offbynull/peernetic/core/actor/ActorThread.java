package com.offbynull.peernetic.core.actor;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.actor.ActorRunnable.AddShuttleMessage;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
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
    
    public static ActorThread create(String prefix) {
        // create runnable
        Bus bus = new Bus();
        ActorRunnable runnable = new ActorRunnable(prefix, bus);
        
        Shuttle selfShuttle = runnable.getIncomingShuttle();

        // add in our own shuttle as well so we can send msgs to ourselves
        bus.add(new AddShuttleMessage(selfShuttle));

        // start thread
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName(ActorRunnable.class.getSimpleName());
        thread.start();

        // return
        return new ActorThread(thread, runnable);
    }
    
    public void interruptThread() {
        thread.interrupt();
    }

    public Thread getThread() {
        return thread;
    }

    public Shuttle getIncomingShuttle() {
        return actorRunnable.getIncomingShuttle();
    }

    public void addActor(String id, Actor actor, Object... primingMessages) {
        actorRunnable.addActor(id, actor, primingMessages);
    }

    public void addCoroutineActor(String id, Coroutine coroutine, Object... primingMessages) {
        actorRunnable.addCoroutineActor(id, coroutine, primingMessages);
    }

    public void removeActor(String id) {
        actorRunnable.removeActor(id);
    }

    public void addOutgoingShuttle(Shuttle shuttle) {
        actorRunnable.addOutgoingShuttle(shuttle);
    }

    public void removeShuttle(String prefix) {
        actorRunnable.removeShuttle(prefix);
    }
    
}
