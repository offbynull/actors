package com.offbynull.peernetic.common.concurrent.pumps.queue;

import com.offbynull.peernetic.common.concurrent.pump.PumpWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.Validate;

public final class QueuePumpWriter<T> implements PumpWriter<T> {
    private LinkedBlockingQueue<Iterator<T>> queue;
    private AtomicBoolean closed;

    QueuePumpWriter(AtomicBoolean closed, LinkedBlockingQueue<Iterator<T>> queue) {
        Validate.notNull(queue);
        Validate.notNull(closed);

        this.queue = queue;
    }

    @Override
    public void push(Collection<T> data) {
        Validate.validState(!closed.get());
        Validate.noNullElements(data);
        
        queue.add(Collections.unmodifiableList(new ArrayList<>(data)).iterator());
    }
}
