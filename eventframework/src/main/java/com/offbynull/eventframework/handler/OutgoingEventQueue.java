package com.offbynull.eventframework.handler;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class OutgoingEventQueue {

    private Set<Class<? extends OutgoingEvent>> acceptedTypes;
    private LinkedList<OutgoingEvent> list;
    private Lock lock;
    private Condition condition;

    public OutgoingEventQueue(
            Set<Class<? extends OutgoingEvent>> acceptedTypes) {
        list = new LinkedList<>();
        lock = new ReentrantLock();
        condition = lock.newCondition();
        this.acceptedTypes = Collections.unmodifiableSet(
                new HashSet<>(acceptedTypes));
    }

    public void push(Collection<OutgoingEvent> events) {
        if (events == null || events.contains(null)) {
            throw new NullPointerException();
        }

        for (OutgoingEvent event : events) {
            if (!acceptedTypes.contains(event.getClass())) {
                throw new IllegalArgumentException();
            }
        }

        lock.lock();
        try {
            list.addAll(events);
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void push(OutgoingEvent event) {
        if (event == null) {
            throw new NullPointerException();
        }

        if (!acceptedTypes.contains(event.getClass())) {
            throw new IllegalArgumentException();
        }

        lock.lock();
        try {
            list.add(event);
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void waitForEvents(List<OutgoingEvent> dest)
            throws InterruptedException {
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
