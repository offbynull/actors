package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.ActorStartSettings;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PullQueue;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.helpers.RequestManager;
import com.offbynull.peernetic.actor.helpers.RequestManager.IncomingRequestHandler;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.RouteResult;
import com.offbynull.peernetic.overlay.chord.messages.GetClosestPrecedingFinger;
import com.offbynull.peernetic.overlay.chord.messages.GetClosestPrecedingFingerReply;
import com.offbynull.peernetic.overlay.chord.messages.GetPredecessor;
import com.offbynull.peernetic.overlay.chord.messages.GetPredecessorReply;
import com.offbynull.peernetic.overlay.chord.messages.GetSuccessor;
import com.offbynull.peernetic.overlay.chord.messages.GetSuccessorReply;
import com.offbynull.peernetic.overlay.chord.messages.Notify;
import com.offbynull.peernetic.overlay.chord.messages.NotifyReply;
import com.offbynull.peernetic.overlay.chord.tasks.InitializeTask;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.IdUtils;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.security.SecureRandom;
import java.util.Map;
import org.apache.commons.lang3.Validate;

public final class ChordOverlay<A> extends Actor {
    private Pointer<A> self;
    private Pointer<A> bootstrap;
    private EndpointFinder<A> finder;
    private SecureRandom secureRandom;
    
    private ChordTask<A> chordTask;

    public ChordOverlay(Pointer<A> self, Pointer<A> bootstrap, EndpointFinder<A> finder) {
        Validate.notNull(self);
        Validate.notNull(bootstrap);
        Validate.notNull(finder);
        Validate.isTrue(self.getId().getLimitAsBigInteger().equals(bootstrap.getId().getLimitAsBigInteger()));
        IdUtils.ensureLimitPowerOfTwo(self);
        
        this.self = self;
        this.bootstrap = bootstrap;
        this.finder = finder;
    }

    public ChordOverlay(Pointer<A> self) {
        Validate.notNull(self);
        IdUtils.ensureLimitPowerOfTwo(self);
        
        this.self = self;
    }
    
    @Override
    protected ActorStartSettings onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {        
        secureRandom = SecureRandom.getInstance("SUN", "SHA1PRNG");
        chordTask = new ChordTask<>(self, bootstrap, secureRandom, finder);

        
        return new ActorStartSettings(timestamp); // hit immediately
    }

    @Override
    protected long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue, Endpoint selfEndpoint) throws Exception {
        long nextHitTime = Long.MAX_VALUE;
        boolean hit = false;
        
        Incoming incoming;
        while ((incoming = pullQueue.pull()) != null) {
            hit = true;
            long stepNextHitTime = chordTask.process(timestamp, incoming, pushQueue);
            nextHitTime = Math.min(nextHitTime, stepNextHitTime);
        }
        
        if (!hit) {
            nextHitTime = chordTask.process(timestamp, null, pushQueue);
        }
        
        return nextHitTime;
    }

    @Override
    protected void onStop(long timestamp, PushQueue pushQueue) throws Exception {
    }
}
