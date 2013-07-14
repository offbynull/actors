package com.offbynull.p2prpc;

import java.io.IOException;
import java.net.InetSocketAddress;

public interface Client {
    void start() throws IOException;
    Object transmitRpcCall(InetSocketAddress address, InvokeData data)
             throws IOException;
    void stop() throws IOException;
}