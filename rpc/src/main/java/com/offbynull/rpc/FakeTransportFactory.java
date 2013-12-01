package com.offbynull.rpc;

import com.offbynull.rpc.transport.Transport;
import com.offbynull.rpc.transport.fake.FakeHub;
import com.offbynull.rpc.transport.fake.FakeTransport;
import java.io.IOException;
import org.apache.commons.lang3.Validate;

public final class FakeTransportFactory<A> implements TransportFactory<A> {

    private FakeHub<A> hub;
    private long timeout = 10000;
    private A address;

    public FakeTransportFactory(FakeHub<A> hub, A address) {
        Validate.notNull(hub);
        Validate.notNull(address);
        
        this.hub = hub;
        this.address = address;
    }

    public FakeHub<A> getHub() {
        return hub;
    }

    public void setHub(FakeHub<A> hub) {
        Validate.notNull(hub);
        this.hub = hub;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);
        this.timeout = timeout;
    }

    public A getAddress() {
        return address;
    }

    public void setAddress(A address) {
        Validate.notNull(address);
        this.address = address;
    }
    
    @Override
    public Transport<A> createTransport() throws IOException {
        return new FakeTransport<>(address, hub, timeout);
    }
    
}
