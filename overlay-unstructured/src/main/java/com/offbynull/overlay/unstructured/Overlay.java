package com.offbynull.overlay.unstructured;

import com.offbynull.peernetic.rpc.Rpc;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

public final class Overlay<A> {
    private Rpc<A> rpc;
    private IncomingLinkMaintainer<A> incomingLinkMaintainer;
    private OutgoingLinkMaintainer<A> outgoingLinkMaintainer;
    private OverlayService<A> service;
    private Lock lock = new ReentrantLock();
    private State state = State.UNKNOWN;
    
    public void start(Rpc<A> rpc, A bootstrap, int maxInLinks, int maxOutLinks, OverlayListener<A> overlayListener)
            throws InterruptedException {
        Validate.notNull(rpc);
        Validate.notNull(bootstrap);
        Validate.notNull(overlayListener);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, maxInLinks);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, maxOutLinks);

        lock.lock();
        try {
            Validate.validState(state == State.UNKNOWN);
            
            incomingLinkMaintainer = new IncomingLinkMaintainer<>(maxInLinks, overlayListener);
            outgoingLinkMaintainer = new OutgoingLinkMaintainer<>(bootstrap, rpc, maxOutLinks, overlayListener);
            service = new OverlayServiceImplementation<>(incomingLinkMaintainer, outgoingLinkMaintainer);

            try {
                incomingLinkMaintainer.start();
                outgoingLinkMaintainer.start();

                rpc.addService(OverlayService.SERVICE_ID, service);
            } catch (RuntimeException e) {
                state = State.STARTED; // required by stop();
                stop();
                throw e;
            }
            
            state = State.STARTED;
        } finally {
            lock.unlock();
        }
    }
    
    public void stop() throws InterruptedException {
        lock.lock();
        try {
            Validate.validState(state == State.STARTED);
            
            try {
                rpc.removeService(OverlayService.SERVICE_ID);
            } catch (RuntimeException re) {
                // do nothing
            }

            state = State.STOPPED;
            
            try {
                incomingLinkMaintainer.stop();
            } catch (RuntimeException re) {
                // do nothing
            }

            try {
                outgoingLinkMaintainer.stop();
            } catch (RuntimeException re) {
                // do nothing
            }
        } finally {
            lock.unlock();
        }
    }
    
    private enum State {
        UNKNOWN,
        STARTED,
        STOPPED
    }
}
