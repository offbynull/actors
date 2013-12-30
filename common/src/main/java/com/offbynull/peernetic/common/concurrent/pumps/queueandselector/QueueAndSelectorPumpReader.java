package com.offbynull.peernetic.common.concurrent.pumps.queueandselector;

import com.offbynull.peernetic.common.concurrent.pump.PumpReader;
import java.io.IOException;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.commons.lang3.Validate;

public class QueueAndSelectorPumpReader<T> implements PumpReader<T> {
    private LinkedBlockingQueue<Iterator<T>> queue;
    private AtomicBoolean closed;
    private Selector selector;

    QueueAndSelectorPumpReader(AtomicBoolean closed, LinkedBlockingQueue<Iterator<T>> queue, Selector selector) {
        Validate.notNull(queue);
        Validate.notNull(closed);
        Validate.notNull(selector);

        this.queue = queue;
        this.selector = selector;
    }

    @Override
    public Iterator<T> pull(long timeout) throws InterruptedException, IOException {
        Validate.validState(!closed.get());
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, timeout);

        if (timeout == 0L) {
            selector.selectNow();
        } else {
            selector.select(timeout);
        }
        
        LinkedList<Iterator<T>> dst = new LinkedList<>();
        queue.drainTo(dst);
        
        IteratorChain<T> chain = new IteratorChain();
        for (Iterator<T> batch : dst) {
            chain.addIterator(batch);
        }
        
        return IteratorUtils.unmodifiableIterator(chain);
    }
    
}
