package com.offbynull.peernetic.common.concurrent.pumps.queue;

import com.offbynull.peernetic.common.concurrent.pump.PumpReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.commons.lang3.Validate;

public class QueuePumpReader<T> implements PumpReader<T> {
    private LinkedBlockingQueue<Iterator<T>> queue;
    private AtomicBoolean closed;

    QueuePumpReader(AtomicBoolean closed, LinkedBlockingQueue<Iterator<T>> queue) {
        Validate.notNull(queue);
        Validate.notNull(closed);

        this.queue = queue;
    }

    @Override
    public Iterator<T> pull(long timeout) throws InterruptedException {
        Validate.validState(!closed.get());
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, timeout);
        
        LinkedList<Iterator<T>> dst = new LinkedList<>();
        Iterator<T> first = queue.poll(timeout, TimeUnit.MILLISECONDS);
        
        if (first == null) {
            return IteratorUtils.emptyIterator();
        }

        dst.add(first);
        queue.drainTo(dst);
        
        IteratorChain<T> chain = new IteratorChain();
        for (Iterator<T> batch : dst) {
            chain.addIterator(batch);
        }
        
        return IteratorUtils.unmodifiableIterator(chain);
    }
    
}
