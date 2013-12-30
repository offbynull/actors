package com.offbynull.peernetic.common.concurrent.lifecycle;

public final class RetainStateLifeCycleListener implements LifeCycleListener {
    private volatile LifeCycleState state = LifeCycleState.CREATED;

    @Override
    public void stateChanged(LifeCycle service, LifeCycleState state) {
        this.state = state;
    }
    
    public LifeCycleState getState() {
        return state;
    }
}
