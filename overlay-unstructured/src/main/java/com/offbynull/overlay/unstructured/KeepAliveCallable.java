package com.offbynull.overlay.unstructured;

import com.offbynull.overlay.unstructured.OverlayService;
import com.offbynull.overlay.unstructured.KeepAliveListener.FailReason;
import com.offbynull.rpc.Rpc;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.Validate;

final class KeepAliveCallable<A> implements Callable<Void> {
    private byte[] secret;
    private A address;
    private Rpc<A> rpc;
    private KeepAliveListener<A> listener;
    private long delay;
    private int maxRetries;

    public KeepAliveCallable(A address, Rpc<A> rpc, ByteBuffer secret, KeepAliveListener<A> listener, long delay, int maxRetries) {
        Validate.notNull(address);
        Validate.notNull(rpc);
        Validate.notNull(secret);
        Validate.notNull(listener);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, delay);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxRetries);
        Validate.isTrue(secret.remaining() == OverlayService.SECRET_SIZE);
        
        
        this.address = address;
        this.rpc = rpc;
        this.secret = new byte[secret.remaining()];
        this.listener = listener;
        this.delay = delay;
        this.maxRetries = maxRetries;
        
        secret.get(this.secret);
    }

    @Override
    public Void call() throws Exception {
        OverlayService<A> node = rpc.accessService(address, OverlayService.SERVICE_ID, OverlayService.class);
        
        int failCount = -1;
        while (failCount < maxRetries) {
            try {
                if (!node.keepAlive(secret)) {
                    listener.keepAliveFailed(address, FailReason.REJECTED_RESPONSE);
                    return null;
                } else {
                    listener.keepAliveSuccessful(address);
                }
                failCount = -1;
            } catch (RuntimeException re) {
                failCount++;
            }
            
            Thread.sleep(delay);
        }
        
        listener.keepAliveFailed(address, FailReason.NO_RESPONSE);
        
        return null;
    }
    
}
