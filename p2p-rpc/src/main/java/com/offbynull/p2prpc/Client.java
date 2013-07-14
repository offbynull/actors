package com.offbynull.p2prpc;

import com.offbynull.p2prpc.invoke.InvokeData;
import java.io.IOException;
import java.net.InetSocketAddress;

public interface Client {
    void start() throws IOException;
    Object transmitRpcCall(InetSocketAddress address, InvokeData data)
             throws IOException;
    void stop() throws IOException;
}