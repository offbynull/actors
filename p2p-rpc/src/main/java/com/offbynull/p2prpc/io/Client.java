package com.offbynull.p2prpc.io;

import java.io.IOException;
import java.net.InetSocketAddress;

public interface Client {
    void start() throws IOException;
    byte[] send(InetSocketAddress address, byte[] data) throws IOException;
    void stop() throws IOException;
}