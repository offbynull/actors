package com.offbynull.peernetic.actor;

public interface Endpoint {
    void send(Endpoint source, Object message);
}
