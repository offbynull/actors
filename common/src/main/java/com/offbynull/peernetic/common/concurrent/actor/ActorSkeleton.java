package com.offbynull.peernetic.common.concurrent.actor;

import java.util.Map;

public interface ActorSkeleton {
    ActorQueue onStart(long timestamp, Map<Object, Object> dataMap);
    long onStep(long timestamp);
    void onStop();
}
