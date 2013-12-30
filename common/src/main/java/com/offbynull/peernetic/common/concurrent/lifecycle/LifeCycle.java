package com.offbynull.peernetic.common.concurrent.lifecycle;

public interface LifeCycle {
    void setListener(LifeCycleListener listener);
    LifeCycleListener getListener();
    void onStart(Object ... init) throws Exception;
    void onProcess() throws Exception;
    void onStop() throws Exception;
    void triggerStop();
}
