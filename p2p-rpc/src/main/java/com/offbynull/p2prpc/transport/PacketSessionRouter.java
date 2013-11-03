package com.offbynull.p2prpc.transport;

import java.util.Map;

public final class PacketSessionRouter {
    private TimeoutTracker<PacketId> timeoutTracker;
    private Map<PacketId, ServerResponseCallback> sessions;
}
