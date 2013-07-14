package com.offbynull.p2prpc;

import com.offbynull.p2prpc.invoke.Invoker;
import java.io.IOException;

public interface Server {
    void start(Invoker invoker) throws IOException;
    void stop() throws IOException;
}
