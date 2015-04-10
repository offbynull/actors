package com.offbynull.peernetic.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;

public final class CaptureShuttle implements Shuttle {

    private final String prefix;
    private final LinkedBlockingQueue<Message> queuedMessages;

    public CaptureShuttle(String prefix) {
        Validate.notNull(prefix);
        this.prefix = prefix;
        this.queuedMessages = new LinkedBlockingQueue<>();
    }
    
    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        
        queuedMessages.addAll(messages);
    }
    
    public List<Message> drainMessages() {
        List<Message> ret = new ArrayList<>();
        queuedMessages.drainTo(ret);
        return ret;
    }
}
