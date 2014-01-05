package com.offbynull.peernetic.actor;

import org.apache.commons.lang3.Validate;

public final class RemoteEndpoint<A> implements Endpoint {
    private A address;

    public RemoteEndpoint(A address) {
        Validate.notNull(address);
        this.address = address;
    }

    public A getAddress() {
        return address;
    }
    
}
