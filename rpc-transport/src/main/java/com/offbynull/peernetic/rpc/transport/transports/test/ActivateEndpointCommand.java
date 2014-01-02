package com.offbynull.peernetic.rpc.transport.transports.test;

import com.offbynull.peernetic.common.concurrent.actor.ActorQueueWriter;
import org.apache.commons.lang3.Validate;

final class ActivateEndpointCommand<A> {
    private A address;
    private ActorQueueWriter writer;

    public ActivateEndpointCommand(A address, ActorQueueWriter writer) {
        Validate.notNull(address);
        Validate.notNull(writer);
        
        this.writer = writer;
        this.address = address;
    }

    public A getAddress() {
        return address;
    }

    public ActorQueueWriter getWriter() {
        return writer;
    }
    
}
