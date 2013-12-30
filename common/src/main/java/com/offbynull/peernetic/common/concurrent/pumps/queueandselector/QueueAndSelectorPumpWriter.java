package com.offbynull.peernetic.common.concurrent.pumps.queueandselector;

import com.offbynull.peernetic.common.concurrent.pump.PumpWriter;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.Validate;

public final class QueueAndSelectorPumpWriter<T> implements PumpWriter<T> {
    private LinkedBlockingQueue<Iterator<T>> queue;
    private AtomicBoolean closed;
    private Selector selector;

    QueueAndSelectorPumpWriter(AtomicBoolean closed, LinkedBlockingQueue<Iterator<T>> queue, Selector selector) {
        Validate.notNull(queue);
        Validate.notNull(closed);
        Validate.notNull(selector);

        this.queue = queue;
        this.selector = selector;
    }

    @Override
    public void push(Collection<T> data) {
        Validate.validState(!closed.get());
        Validate.noNullElements(data);
        
        queue.add(Collections.unmodifiableList(new ArrayList<>(data)).iterator());
        selector.wakeup();
    }
}
