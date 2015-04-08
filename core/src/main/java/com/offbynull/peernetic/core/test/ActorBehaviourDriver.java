package com.offbynull.peernetic.core.test;

import java.time.Duration;

public interface ActorBehaviourDriver {
    Duration calculateDuration(String address, Object message, Duration realDuration);
}
