package com.offbynull.peernetic.rpc.transport.internal;

import com.offbynull.peernetic.common.concurrent.actor.ActorQueueWriter;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseListener;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class DefaultIncomingResponseListener implements IncomingMessageResponseListener {
    
    private Object msgId;
    private ActorQueueWriter writer;

    public DefaultIncomingResponseListener(Object msgId, ActorQueueWriter writer) {
        Validate.notNull(writer);
        this.writer = writer;
    }

    @Override
    public void responseReady(ByteBuffer response) {
        Message msg = Message.createResponseMessage(msgId, new SendResponseCommand(response));
        writer.push(msg);
    }

    @Override
    public void terminate() {
        Message msg = Message.createResponseMessage(msgId, new DropResponseCommand());
        writer.push(msg);
    }
}
