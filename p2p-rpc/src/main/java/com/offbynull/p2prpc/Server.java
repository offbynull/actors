package com.offbynull.p2prpc;

import java.io.IOException;

public interface Server {
    void start(Invoker invoker) throws IOException;
    void stop() throws IOException;
}
