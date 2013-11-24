package com.offbynull.rpc.transport.fake;

final class FakeEndpoint<A> {
    private FakeHubReceiver<A> receiver;
    private boolean active;

    public FakeEndpoint(FakeHubReceiver<A> receiver) {
        this.receiver = receiver;
    }

    public FakeHubReceiver<A> getReceiver() {
        return receiver;
    }

    public void setReceiver(FakeHubReceiver<A> receiver) {
        this.receiver = receiver;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
}
