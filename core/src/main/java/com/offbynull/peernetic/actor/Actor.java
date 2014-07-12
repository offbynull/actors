package com.offbynull.peernetic.actor;

import java.time.Instant;

public interface Actor {
    void onStart(Instant time) throws Exception;
    void onStep(Instant time, Endpoint source, Object message) throws Exception;
    void onStop(Instant time) throws Exception;
}
