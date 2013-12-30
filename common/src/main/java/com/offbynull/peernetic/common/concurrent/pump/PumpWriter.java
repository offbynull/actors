package com.offbynull.peernetic.common.concurrent.pump;

import java.io.IOException;
import java.util.Collection;

public interface PumpWriter<T> {
    void push(Collection<T> data) throws InterruptedException, IOException;
}
