package com.offbynull.peernetic.core.test;

import java.time.Duration;

public final class SimpleMessageBehaviourDriver implements MessageBehaviourDriver {

    @Override
    public Duration calculateDuration(String fromAddress, String toAddress, Object message) {
        return Duration.ZERO;
    }
    
}
