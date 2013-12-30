package com.offbynull.peernetic.common.concurrent.pump;

import java.io.Closeable;

public interface ReadablePump<T> extends Closeable {
    PumpReader<T> getPumpReader();
}
