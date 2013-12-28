package com.offbynull.peernetic.common.concurrent.service;

public interface ServiceListener {
    void stateChanged(Service service, ServiceState state);
}
