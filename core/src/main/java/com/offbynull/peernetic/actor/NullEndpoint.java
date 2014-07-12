package com.offbynull.peernetic.actor;

public final class NullEndpoint implements Endpoint {
    
    public static final NullEndpoint INSTANCE = new NullEndpoint();

    @Override
    public void send(Endpoint source, Object message) {
        // Do nothing
    }
}
