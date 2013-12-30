package com.offbynull.peernetic.common.concurrent.pumps.queue;

import com.offbynull.peernetic.common.concurrent.pump.PumpWriter;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import org.apache.commons.lang3.Validate;

public final class QueuePumpWriter<T> implements PumpWriter<T> {
    private volatile boolean closed;
    private BlockingQueue<T> queue;

    public QueuePumpWriter(BlockingQueue<T> queue) {
        Validate.notNull(queue);

        this.queue = queue;
    }

    @Override
    public void push(Collection<T> data) {
        Validate.isTrue(!closed);
        
        queue.addAll(data);
    }

    @Override
    public void close() {
        closed = true;
    }
}
