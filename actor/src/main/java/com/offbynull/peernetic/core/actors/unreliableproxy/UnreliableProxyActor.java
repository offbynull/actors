package com.offbynull.peernetic.core.actors.unreliableproxy;

import com.offbynull.peernetic.core.actor.Actor;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.CoroutineActor;

public final class UnreliableProxyActor implements Actor {

    private CoroutineActor actor = new CoroutineActor(new UnreliableProxyCoroutine());
    
    @Override
    public boolean onStep(Context context) throws Exception {
        return actor.onStep(context);
    }
    
}
