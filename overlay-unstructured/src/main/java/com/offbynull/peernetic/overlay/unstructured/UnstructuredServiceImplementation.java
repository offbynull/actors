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
package com.offbynull.peernetic.overlay.unstructured;

import com.offbynull.peernetic.common.concurrent.actor.ActorQueueWriter;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.rpc.RpcInvokeKeys;
import com.offbynull.peernetic.rpc.invoke.InvokeThreadInformation;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

final class UnstructuredServiceImplementation<A> implements UnstructuredService<A> {
    private ActorQueueWriter writerToUnstructuredOverlay;

    public UnstructuredServiceImplementation(ActorQueueWriter writerToUnstructuredOverlay) {
        Validate.notNull(writerToUnstructuredOverlay);
        this.writerToUnstructuredOverlay = writerToUnstructuredOverlay;
    }

    @Override
    public State<A> getState() {
        DefaultCommandResponseListener<State<A>> listener = new DefaultCommandResponseListener<>();
        writerToUnstructuredOverlay.push(Message.createOneWayMessage(
                new GetStateCommand<>(listener)));
        return listener.waitForResponse();
    }

    @Override
    public boolean join(byte[] secret) {
        Validate.isTrue(SECRET_SIZE == secret.length);
        
        A from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
        
        DefaultCommandResponseListener<Boolean> listener = new DefaultCommandResponseListener<>();
        writerToUnstructuredOverlay.push(Message.createOneWayMessage(
                new InitiateJoinCommand<>(from, ByteBuffer.wrap(secret), listener)));
        return listener.waitForResponse();
    }

    @Override
    public boolean keepAlive(byte[] secret) {
        Validate.isTrue(SECRET_SIZE == secret.length);
        
        A from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
        
        DefaultCommandResponseListener<Boolean> listener = new DefaultCommandResponseListener<>();
        writerToUnstructuredOverlay.push(Message.createOneWayMessage(
                new InitiateKeepAliveCommand<>(from, ByteBuffer.wrap(secret), listener)));
        return listener.waitForResponse();
    }
}
