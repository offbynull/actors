package com.offbynull.rpc;

import com.offbynull.rpc.transport.Transport;
import java.io.IOException;

/**
 * A factory class that creates {@link Transport}s.
 * @author Kasra F
 * @param <A> address type
 */
public interface TransportFactory<A> {
    /**
     * Creates a {@link Transport}.
     * @return a transport
     * @throws IOException on error 
     */
    Transport<A> createTransport() throws IOException;
}
