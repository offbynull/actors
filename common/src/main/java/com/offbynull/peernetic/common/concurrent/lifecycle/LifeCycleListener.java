package com.offbynull.peernetic.common.concurrent.lifecycle;

public interface LifeCycleListener {
    void stateChanged(LifeCycle service, LifeCycleState state);
}
