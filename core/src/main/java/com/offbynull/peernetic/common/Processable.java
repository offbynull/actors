package com.offbynull.peernetic.common;

import java.time.Duration;
import java.time.Instant;

public interface Processable {
    Duration process(Instant time);
}
