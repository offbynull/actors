package com.offbynull.overlay.unstructured.tasks;

import com.offbynull.overlay.unstructured.services.OverlayService;
import com.offbynull.overlay.unstructured.tasks.JoinListener.FailReason;
import com.offbynull.rpc.Rpc;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.Validate;

final class JoinCallable<A> implements Callable<Void> {
    private A address;
    private Rpc<A> rpc;
    private JoinListener<A> listener;
    private int maxRetries;

    public JoinCallable(A address, Rpc<A> rpc, JoinListener<A> listener, int maxRetries) {
        Validate.notNull(address);
        Validate.notNull(rpc);
        Validate.notNull(listener);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxRetries);
        
        
        this.address = address;
        this.rpc = rpc;
        this.listener = listener;
        this.maxRetries = maxRetries;
    }
    
    @Override
    public Void call() throws Exception {
        Random random = new Random();
        
        byte[] secret = new byte[OverlayService.SECRET_SIZE];
        random.nextBytes(secret);
        
        OverlayService<A> node = rpc.accessService(address, OverlayService.SERVICE_ID, OverlayService.class);
        
        int failCount = -1;
        while (failCount < maxRetries) {
            try {
                if (!node.join(secret)) {
                    listener.joinFailed(address, FailReason.REJECTED_RESPONSE);
                    return null;
                } else {
                    listener.joinComplete(address, ByteBuffer.wrap(secret).asReadOnlyBuffer());
                    return null;
                }
            } catch (RuntimeException re) {
                failCount++;
            }
        }
        
        listener.joinFailed(address, FailReason.NO_RESPONSE);
        
        return null;
    }
    
}
