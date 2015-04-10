package com.offbynull.peernetic.core.gateway;

import com.offbynull.peernetic.core.Shuttle;

public interface InputGateway extends Gateway {
    Shuttle getIncomingShuttle();
}
