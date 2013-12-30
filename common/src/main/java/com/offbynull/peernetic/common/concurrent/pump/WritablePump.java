package com.offbynull.peernetic.common.concurrent.pump;

import java.io.Closeable;

public interface WritablePump<T> extends Closeable {
    PumpWriter<T> getPumpWriter();
}
