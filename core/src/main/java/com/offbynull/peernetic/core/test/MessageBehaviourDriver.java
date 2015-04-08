package com.offbynull.peernetic.core.test;

import java.time.Duration;

public interface MessageBehaviourDriver {
    Duration calculateDuration(String fromAddress, String toAddress, Object message);
}
