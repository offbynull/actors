package com.offbynull.peernetic.core.gateway;

import com.offbynull.peernetic.core.Shuttle;

public interface Gateway extends AutoCloseable {

    Shuttle getIncomingShuttle();
    
}
