package com.offbynull.peernetic.actor;

import java.time.Instant;

@FunctionalInterface
public interface Actor {
    default void onStart(Instant time) throws Exception { };
    void onStep(Instant time, Endpoint source, Object message) throws Exception;
    default void onStop(Instant time) throws Exception { };
}
