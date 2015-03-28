package com.offbynull.peernetic.actor;

import java.time.Instant;

@FunctionalInterface
public interface Actor {
    void onStep(Instant time, Endpoint source, Object message) throws Exception;
}
