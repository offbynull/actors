package com.offbynull.peernetic.common.concurrent.pumps.queue;

import com.offbynull.peernetic.common.concurrent.pump.ReadablePump;
import com.offbynull.peernetic.common.concurrent.pump.WritablePump;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class QueuePump<T> implements ReadablePump<T>, WritablePump<T> {
    private AtomicBoolean closed = new AtomicBoolean(false);
    private LinkedBlockingQueue<Iterator<T>> internalQueue = new LinkedBlockingQueue<>();
    
    @Override
    public QueuePumpReader<T> getPumpReader() {
        return new QueuePumpReader<>(closed, internalQueue);
    }
    
    @Override
    public QueuePumpWriter<T> getPumpWriter() {
        return new QueuePumpWriter<>(closed, internalQueue);
    }
    
    @Override
    public void close() {
        closed.set(true);
    }
}
