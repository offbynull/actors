package com.offbynull.peernetic.actor;

import java.io.Closeable;
import java.time.Duration;

public interface EndpointScheduler extends Closeable {
    void scheduleMessage(Duration delay, Endpoint source, Endpoint destination, Object message);    
}
