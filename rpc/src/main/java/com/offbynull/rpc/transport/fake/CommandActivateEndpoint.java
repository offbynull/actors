package com.offbynull.rpc.transport.fake;

import org.apache.commons.lang3.Validate;

final class CommandActivateEndpoint<A> implements Command {
    private A address;

    public CommandActivateEndpoint(A address) {
        Validate.notNull(address);
        
        this.address = address;
    }

    public A getAddress() {
        return address;
    }
}
