package com.offbynull.peernetic.javaflow;

import com.offbynull.peernetic.actor.Endpoint;
import java.time.Instant;

public interface JavaflowTask extends Runnable {

    void setMessage(Object message);

    void setSource(Endpoint source);

    void setTime(Instant time);
    
    // must return the same object each time this is called. the object returned gets updated as setters above are called.
    TaskState getState();
    
}
