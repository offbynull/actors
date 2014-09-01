package com.offbynull.peernetic.playground.unstructuredmesh;

import com.offbynull.peernetic.actor.Endpoint;
import java.time.Instant;

public interface ContinuableActor extends Runnable {

    void setMessage(Object message);

    void setSource(Endpoint source);

    void setTime(Instant time);
    
}
