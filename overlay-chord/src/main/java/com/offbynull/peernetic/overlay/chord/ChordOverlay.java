package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.ActorStartSettings;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.PullQueue;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.Map;

public final class ChordOverlay<A> extends Actor {
    private ChordState<A> chordState;
    private Pointer<A> selfPtr;
    private A bootstrap;

    @Override
    protected ActorStartSettings onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {
        chordState = new ChordState<>(selfPtr);

        return new ActorStartSettings();
    }

    @Override
    protected long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue, Endpoint selfEndpoint) throws Exception {
        return Long.MAX_VALUE;
    }

    @Override
    protected void onStop(long timestamp, PushQueue pushQueue) throws Exception {
    }
    
}
