package com.offbynull.rpc.transport.fake;

import org.apache.commons.lang3.Validate;

final class CommandSend<A> implements Command {
    private Message<A> message;

    public CommandSend(Message<A> message) {
        Validate.notNull(message);
        
        this.message = message;
    }

    public Message<A> getMessage() {
        return message;
    }
}
