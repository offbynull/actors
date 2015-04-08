package com.offbynull.peernetic.core.test;

import java.time.Duration;

public final class SimpleActorBehaviourDriver implements ActorBehaviourDriver {

    @Override
    public Duration calculateDuration(String address, Object message, Duration realDuration) {
        return Duration.ZERO;
    }
    
}
