package com.offbynull.peernetic.common.concurrent.pumps.queueandselector;

import com.offbynull.peernetic.common.concurrent.pump.ReadablePump;
import com.offbynull.peernetic.common.concurrent.pump.WritablePump;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.Validate;

public final class QueueAndSelectorPump<T> implements ReadablePump<T>, WritablePump<T> {
    private AtomicBoolean closed = new AtomicBoolean(false);
    private LinkedBlockingQueue<Iterator<T>> internalQueue = new LinkedBlockingQueue<>();
    private Selector selector;

    public QueueAndSelectorPump(Selector selector) {
        Validate.notNull(selector);
        
        this.selector = selector;
    }
    
    @Override
    public QueueAndSelectorPumpReader<T> getPumpReader() {
        return new QueueAndSelectorPumpReader<>(closed, internalQueue, selector);
    }
    
    @Override
    public QueueAndSelectorPumpWriter<T> getPumpWriter() {
        return new QueueAndSelectorPumpWriter<>(closed, internalQueue, selector);
    }
    
    @Override
    public void close() {
        closed.set(true);
    }
}
