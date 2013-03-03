package com.offbynull.eventframework.tests;

import com.offbynull.eventframework.ClientResultListener;
import com.offbynull.eventframework.ClientResultListener;

public final class BasicClientResultListener
        implements ClientResultListener {

    private volatile ResultType type;
    private volatile Object finishedResult;
    private volatile Throwable throwable;

    public BasicClientResultListener() {
    }

    @Override
    public void processorFinished(long timestamp, Object result) {
        type = ResultType.FINISHED;
        finishedResult = result;
    }

    @Override
    public void processorErrored(long timestamp, String description) {
        type = ResultType.ERRORED;
    }

    @Override
    public void processorException(Throwable throwable) {
        type = ResultType.EXCEPTION;
        this.throwable = throwable;
    }

    public ResultType getResultType() throws InterruptedException {
        return type;
    }
    
    public Object getFinishedResult() throws InterruptedException {
        return finishedResult;
    }
    
    public Throwable getThrowable() throws InterruptedException {
        return throwable;
    }
    
    public enum ResultType {
        FINISHED,
        ERRORED,
        EXCEPTION
    }
}
