package com.offbynull.peernetic.nettyp2p.handlers.queue;

import com.offbynull.peernetic.nettyp2p.handlers.common.AbstractOutgoingTransformHandler;
import java.util.concurrent.BlockingQueue;
import org.apache.commons.lang3.Validate;

/**
 * A Netty handler that writes data that comes in to a {@link BlockingQueue}.
 * @author Kasra Faghihi
 */
public final class WriteToBlockingQueueHandler extends AbstractOutgoingTransformHandler {

    private BlockingQueue<Object> queue;

    /**
     * Constructs a {@link WriteToBlockingQueueHandler} object.
     * @param queue queue to write to
     * @throws NullPointerException if any argument is {@code null}
     */
    public WriteToBlockingQueueHandler(BlockingQueue<Object> queue) {
        Validate.notNull(queue);
        this.queue = queue;
    }
    
    @Override
    protected Object transform(Object obj) {
        Validate.notNull(obj);
        queue.add(obj);
        return obj;
    }
}
