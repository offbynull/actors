package com.offbynull.peernetic.playground.chorddht;

import com.offbynull.peernetic.actor.Endpoint;
import java.time.Instant;

public interface ContinuableTask extends Runnable {

    void setMessage(Object message);

    void setSource(Endpoint source);

    void setTime(Instant time);
    
}
