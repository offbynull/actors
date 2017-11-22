package com.offbynull.actors.core.gateways.actor;

import com.offbynull.actors.core.shuttle.Address;
import com.offbynull.coroutines.user.CoroutineRunner;

public final class SerializableActorHelper {
    private SerializableActorHelper() {
        // do nothing
    }

    public static SerializableActor createFake(String address) {
        Context context = new Context(Address.fromString(address));
        CoroutineRunner runner = new CoroutineRunner(cnt -> {});
        Actor actor = new Actor(null, runner, context);
        
        return SerializableActor.serialize(actor);        
    }
    
    public static SerializableActor createFake(String address, Object checkpointMsg, long timeout) {
        Context context = new Context(Address.fromString(address));
        context.checkpointTimeout(timeout);
        context.checkpointMessage(checkpointMsg);
        CoroutineRunner runner = new CoroutineRunner(cnt -> {});
        Actor actor = new Actor(null, runner, context);
        
        return SerializableActor.serialize(actor);
    }
}
