package com.offbynull.p2prpc;

import com.offbynull.p2prpc.transport.Transport;
import java.io.IOException;

public interface TransportFactory<A> {
    Transport<A> createTransport() throws IOException;
}
