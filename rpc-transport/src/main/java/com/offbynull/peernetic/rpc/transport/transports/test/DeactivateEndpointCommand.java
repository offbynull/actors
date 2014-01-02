package com.offbynull.peernetic.rpc.transport.transports.test;

import org.apache.commons.lang3.Validate;

final class DeactivateEndpointCommand<A> {
    private A address;

    public DeactivateEndpointCommand(A address) {
        Validate.notNull(address);
        
        this.address = address;
    }

    public A getAddress() {
        return address;
    } 
}
