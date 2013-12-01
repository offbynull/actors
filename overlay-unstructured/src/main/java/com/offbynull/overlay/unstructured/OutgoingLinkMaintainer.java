package com.offbynull.overlay.unstructured;

import com.offbynull.overlay.unstructured.OverlayListener.LinkType;
import com.offbynull.rpc.Rpc;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

public final class OutgoingLinkMaintainer<A> {
    private Lock accessLock = new ReentrantLock();
    
    private ExecutorService executor;
    private AtomicInteger slotCount;
    private Rpc<A> rpc;
    private A bootstrap;
    private OverlayListener<A> overlayListener;
    
    private State state = State.UNKNOWN;
    
    public OutgoingLinkMaintainer(A bootstrap, Rpc<A> rpc, int maxJoin, OverlayListener<A> listener) {
        Validate.notNull(bootstrap);
        Validate.notNull(rpc);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, maxJoin);
        Validate.notNull(listener);
        
        slotCount = new AtomicInteger(maxJoin);
        executor = new ThreadPoolExecutor(1, maxJoin + 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        this.bootstrap = bootstrap;
        this.rpc = rpc;
        this.overlayListener = listener;
    }
    
    public void start() {
        accessLock.lock();
        try {
            Validate.validState(state == State.UNKNOWN);

            CustomScanListener listener = new CustomScanListener();
            ScanCallable<A> scanner = new ScanCallable(bootstrap, rpc, slotCount.get(), 1000L, listener);
            executor.submit(scanner);
            
            state = State.STARTED;
        } finally {
            accessLock.unlock();
        }
    }
    
    public void stop() throws InterruptedException {
        accessLock.lock();
        try {
            Validate.validState(state == State.STARTED);
            
            state = State.STOPPED;
            
            executor.shutdownNow();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } finally {
            accessLock.unlock();
        }
    }
    
    private final class CustomScanListener implements ScanListener<A> {

        @Override
        public void scanIterationComplete(Set<A> nodes) {
            for (A address : nodes) {
                int remainingOpenSlots = slotCount.getAndDecrement();
                
                JoinListener<A> listener = new CustomJoinListener();
                JoinCallable<A> joinner = new JoinCallable(address, rpc, listener, 3);
                executor.submit(joinner);
                
                if (remainingOpenSlots == 0) {
                    break;
                }
            }
        }
    }
    
    private final class CustomJoinListener implements JoinListener<A> {

        @Override
        public void joinComplete(A address, ByteBuffer secret) {
            overlayListener.linkEstablished(address, LinkType.OUTGOING);
            
            KeepAliveListener<A> listener = new CustomKeepAliveListener();
            KeepAliveCallable<A> keepAliver = new KeepAliveCallable(address, rpc, secret, listener, 30000L, 3);
            executor.submit(keepAliver);
        }

        @Override
        public void joinFailed(A address, FailReason reason) {
            slotCount.incrementAndGet();
        }
        
    }
    
    private final class CustomKeepAliveListener implements KeepAliveListener<A> {

        @Override
        public void keepAliveSuccessful(A address) {
        }

        @Override
        public void keepAliveFailed(A address, FailReason reason) {
            overlayListener.linkBroken(address, LinkType.OUTGOING);
            
            slotCount.incrementAndGet();
        }
        
    }
    
    private enum State {
        UNKNOWN,
        STARTED,
        STOPPED
    }
}
