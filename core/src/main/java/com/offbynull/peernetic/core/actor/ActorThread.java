package com.offbynull.peernetic.core.actor;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.Message;
import com.offbynull.peernetic.core.Shuttle;
import static com.offbynull.peernetic.core.actor.Actor.MANAGEMENT_ADDRESS;
import com.offbynull.peernetic.core.actor.ActorRunnable.AddShuttleMessage;
import java.util.Collections;
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
        InternalBus internalBus = new InternalBus();
        ActorRunnable runnable = new ActorRunnable(prefix, internalBus);
        
        Shuttle selfShuttle = runnable.getIncomingShuttle();

        // add in our own shuttle as well so we can send msgs to ourselves
        Message messages = new Message(MANAGEMENT_ADDRESS, MANAGEMENT_ADDRESS, new AddShuttleMessage(selfShuttle));
        internalBus.add(Collections.singletonList(messages));

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
