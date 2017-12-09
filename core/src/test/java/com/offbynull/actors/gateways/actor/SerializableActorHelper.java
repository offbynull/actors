package com.offbynull.actors.gateways.actor;

import com.offbynull.actors.address.Address;
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
    
    public static SerializableActor createFake(String address, Object checkpointMsg, long checkpointTimeout) {
        Context context = new Context(Address.fromString(address));
        context.checkpointTimeout(checkpointTimeout);
        context.checkpointPayload(checkpointMsg);
        CoroutineRunner runner = new CoroutineRunner(cnt -> {});
        Actor actor = new Actor(null, runner, context);
        
        return SerializableActor.serialize(actor);
    }
}
