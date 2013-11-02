package com.offbynull.p2prpc.transport;

import java.io.IOException;

public interface Server<A> {
    void start(ServerMessageCallback<A> callback) throws IOException;
    void stop() throws IOException;
}
