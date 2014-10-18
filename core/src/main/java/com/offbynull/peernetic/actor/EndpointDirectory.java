package com.offbynull.peernetic.actor;

public interface EndpointDirectory<T> {
    Endpoint lookup(T address);
}
