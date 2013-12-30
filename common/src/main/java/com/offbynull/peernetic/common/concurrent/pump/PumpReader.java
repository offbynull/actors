package com.offbynull.peernetic.common.concurrent.pump;

import java.io.IOException;
import java.util.Iterator;

public interface PumpReader<T> {
    Iterator<T> pull(long timeout) throws InterruptedException, IOException;
}
