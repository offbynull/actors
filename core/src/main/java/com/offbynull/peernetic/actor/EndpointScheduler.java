package com.offbynull.peernetic.actor;

import java.time.Duration;

public interface EndpointScheduler extends AutoCloseable {
    void scheduleMessage(Duration delay, Endpoint source, Endpoint destination, Object message);    
}
