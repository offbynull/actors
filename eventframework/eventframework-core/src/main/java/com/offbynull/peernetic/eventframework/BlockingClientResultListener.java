package com.offbynull.peernetic.eventframework;

import com.offbynull.peernetic.eventframework.ClientResultListener;
import java.util.concurrent.CountDownLatch;

public final class BlockingClientResultListener
        implements ClientResultListener {

    private CountDownLatch latch;
    private volatile ResultType type;
    private volatile Object finishedResult;
    private volatile Throwable throwable;

    public BlockingClientResultListener() {
        latch = new CountDownLatch(1);
    }

    @Override
    public void processorFinished(long timestamp, Object result) {
        type = ResultType.FINISHED;
        finishedResult = result;
        latch.countDown();
    }

    @Override
    public void processorErrored(long timestamp, String description) {
        type = ResultType.ERRORED;
        latch.countDown();
    }

    @Override
    public void processorException(Throwable throwable) {
        type = ResultType.EXCEPTION;
        this.throwable = throwable;
        latch.countDown();
    }

    public void waitForResult() throws InterruptedException {
        latch.await();
    }
    
    public ResultType getResultType() throws InterruptedException {
        return type;
    }
    
    public Object getFinishedResult() throws InterruptedException {
        latch.await();
        return finishedResult;
    }
    
    public Throwable getThrowable() throws InterruptedException {
        latch.await();
        return throwable;
    }
    
    public enum ResultType {
        FINISHED,
        ERRORED,
        EXCEPTION
    }
}
