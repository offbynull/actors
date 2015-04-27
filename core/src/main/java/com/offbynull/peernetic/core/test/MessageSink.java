package com.offbynull.peernetic.core.test;

import java.io.IOException;
import java.time.Instant;

public interface MessageSink extends AutoCloseable {
    void writeNextMessage(String source, String destination, Instant time, Object message) throws IOException;
}
