package com.offbynull.eventframework.helper;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class SimpleIterateService extends AbstractExecutionThreadService {

    private Thread thread;
    private Object[] objects = new Object[0];

    public final void safeStartAndWait(Object... objects) {
        if (state() != State.NEW) {
            throw new IllegalArgumentException();
        }
        this.objects = objects;
        startAndWait();
    }

    public final void safeStopAndWait() {
        if (state() == State.RUNNING) {
            stopAndWait();
        }
    }

    protected final Object getPassedInObject(int idx) {
        return objects[idx];
    }

    protected abstract boolean iterate() throws Exception;

    @Override
    protected final void run() throws Exception {
        try {
            while (true) {
                if (!iterate()) {
                    return;
                }
            }
        } catch (InterruptedException ie) {
            // Shutdown triggered -- don't rethrow.

            // Do this so it doesn't mess up in shutDown() if blocking method is
            // called
            Thread.interrupted();
        }
    }

    @Override
    protected final void triggerShutdown() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    protected Executor executor() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                thread = Executors.defaultThreadFactory().newThread(command);

                try {
                    thread.setName(serviceName());
                } catch (SecurityException e) {
                    // Not able to set name
                }
                thread.start();
            }
        };
    }
}
