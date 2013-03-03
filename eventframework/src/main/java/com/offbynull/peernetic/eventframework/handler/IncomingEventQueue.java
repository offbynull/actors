package com.offbynull.peernetic.eventframework.handler;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class IncomingEventQueue {
    private LinkedList<IncomingEvent> list;
    private Lock lock;
    private Condition condition;

    public IncomingEventQueue() {
        list = new LinkedList<>();
        lock = new ReentrantLock();
        condition = lock.newCondition();
    }

    public void pushErrorEvent(long sequenceId, String description) {
        push(new DefaultErrorIncomingEvent(sequenceId, description, null));
    }

    public void pushErrorEvent(long sequenceId, Throwable error) {
        push(new DefaultErrorIncomingEvent(sequenceId, error));
    }

    public void push(IncomingEvent event) {
        if (event == null) {
            throw new NullPointerException();
        }

        lock.lock();
        try {
            list.add(event);
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void push(Collection<IncomingEvent> events) {
        if (events == null || events.contains(null)) {
            throw new NullPointerException();
        }

        lock.lock();
        try {
            list.addAll(events);
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void waitForEvents(List<IncomingEvent> dest) throws InterruptedException {
        lock.lock();
        try {
            if (list.isEmpty()) {
                condition.await();
            }
            dest.addAll(list);
            list.clear();
        } finally {
            lock.unlock();
        }
    }
}
