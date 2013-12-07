package com.offbynull.rpc.transport.fake;

import org.apache.commons.lang3.Validate;

final class CommandAddEndpoint<A> implements Command {
    private A address;
    private FakeHubReceiver<A> receiver;

    public CommandAddEndpoint(A address, FakeHubReceiver<A> receiver) {
        Validate.notNull(address);
        Validate.notNull(receiver);
        
        this.address = address;
        this.receiver = receiver;
    }

    public A getAddress() {
        return address;
    }

    public FakeHubReceiver<A> getReceiver() {
        return receiver;
    }
}
