package com.offbynull.peernetic.rpc.transport.internal;

import org.apache.commons.lang3.Validate;

public final class SendMessageCommand<A> {
    private Object content;
    private A destination;

    public SendMessageCommand(Object content, A destination) {
        Validate.notNull(content);
        Validate.notNull(destination);

        this.content = content;
        this.destination = destination;
    }

    public Object getContent() {
        return content;
    }

    public A getDestination() {
        return destination;
    }
    
}
