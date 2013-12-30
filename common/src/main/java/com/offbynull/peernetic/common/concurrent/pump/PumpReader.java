package com.offbynull.peernetic.common.concurrent.pump;

import java.io.Closeable;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public interface PumpReader<T> extends Closeable {
    Collection<T> pull(long timeout, TimeUnit unit) throws InterruptedException;
}
