/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.rpc.transport.internal;

import com.offbynull.peernetic.common.concurrent.actor.ActorQueueWriter;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseListener;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * A {@link IncomingMessageResponseListener} that puts a {@link SendResponseCommand} (on success) or a {@link DropResponseCommand} (on
 * termination) on to a {@link ActorQueueWriter}. The {@link ActorQueueWriter} should point to a {@link TransportActor}.
 * @author User
 */
public final class DefaultIncomingResponseListener implements IncomingMessageResponseListener {
    
    private Object msgId;
    private ActorQueueWriter writer;

    /**
     * Constructs a {@link DefaultIncomingResponseListener} object.
     * @param msgId id of request that the response is to be issued for
     * @param writer writer to the {@link TransportActor} that issued this object
     * @throws NullPointerException if any argument is {@code null}
     */
    public DefaultIncomingResponseListener(Object msgId, ActorQueueWriter writer) {
        Validate.notNull(msgId);
        Validate.notNull(writer);
        
        this.msgId = msgId;
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
