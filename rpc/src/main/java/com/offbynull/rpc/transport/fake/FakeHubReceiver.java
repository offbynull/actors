package com.offbynull.rpc.transport.fake;

public interface FakeHubReceiver<A> {
    void incoming(Packet<A> packet);
}
