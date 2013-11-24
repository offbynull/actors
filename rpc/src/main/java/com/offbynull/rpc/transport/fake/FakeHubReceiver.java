package com.offbynull.rpc.transport.fake;

interface FakeHubReceiver<A> {
    void incoming(Message<A> packet);
}
