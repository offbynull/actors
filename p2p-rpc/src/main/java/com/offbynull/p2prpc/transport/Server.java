package com.offbynull.p2prpc.transport;

import java.io.IOException;

public interface Server {
    void start(ServerMessageCallback callback) throws IOException;
    void stop() throws IOException;
}
