package com.offbynull.peernetic.common.concurrent.pumps.queue;

import com.offbynull.peernetic.common.concurrent.pump.PumpReader;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

public class QueuePumpReader<T> implements PumpReader<T> {
    private volatile boolean closed;
    private BlockingQueue<T> queue;

    public QueuePumpReader(BlockingQueue<T> queue) {
        Validate.notNull(queue);

        this.queue = queue;
    }

    @Override
    public Collection<T> pull(long timeout, TimeUnit unit) throws InterruptedException {
        Validate.isTrue(!closed);
        Validate.notNull(unit);
        
        LinkedList<T> dst = new LinkedList<>();
        T first = queue.poll(timeout, unit);
        
        if (first != null) {
            dst.add(first);
            queue.drainTo(dst);
        }
        
        return Collections.unmodifiableList(dst);
    }

    @Override
    public void close() {
        closed = true;
    }
    
}
