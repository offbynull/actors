package com.offbynull.peernetic.common.concurrent.lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class CompositeLifeCycleListener implements LifeCycleListener {

    private List<LifeCycleListener> listeners;
    
    public CompositeLifeCycleListener(LifeCycleListener ... listeners) {
        Validate.noNullElements(listeners);
        
        this.listeners = new ArrayList<>(Arrays.asList(listeners));
    }
    
    @Override
    public void stateChanged(LifeCycle service, LifeCycleState state) {
        for (LifeCycleListener listener : listeners) {
            try {
                listener.stateChanged(service, state);
            } catch (RuntimeException re) { // NOPMD
                // do nothing
            }
        }
    }
    
}
