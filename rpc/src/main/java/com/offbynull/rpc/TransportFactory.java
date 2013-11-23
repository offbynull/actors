package com.offbynull.rpc;

import com.offbynull.rpc.transport.Transport;
import java.io.IOException;

public interface TransportFactory<A> {
    Transport<A> createTransport() throws IOException;
}
