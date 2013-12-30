package com.offbynull.peernetic.common.concurrent.pump;

import java.io.Closeable;
import java.util.Collection;

public interface PumpWriter<T> extends Closeable {
    void push(Collection<T> data);
}
