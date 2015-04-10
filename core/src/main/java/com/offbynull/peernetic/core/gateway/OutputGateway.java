package com.offbynull.peernetic.core.gateway;

import com.offbynull.peernetic.core.shuttle.Shuttle;

// this is when the gateway can write dynamically to multiple sources, if you're only going to dump to a known number of shuttles that are
// finite, you could implement a basic Gateway and pass thsoe shuttles in through a constructor
public interface OutputGateway extends Gateway {
    void addOutgoingShuttle(Shuttle shuttle);
    void removeOutgoingShuttle(String shuttlePrefix);
}
