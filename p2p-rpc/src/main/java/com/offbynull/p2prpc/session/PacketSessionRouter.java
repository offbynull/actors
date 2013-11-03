package com.offbynull.p2prpc.session;

import java.util.Map;

public final class PacketSessionRouter {
    private TimeoutTracker<PacketId> timeoutTracker;
    private Map<PacketId, ServerResponseCallback> sessions;
}
