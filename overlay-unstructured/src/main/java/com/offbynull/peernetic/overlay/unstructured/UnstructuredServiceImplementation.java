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

import com.offbynull.peernetic.rpc.RpcInvokeKeys;
import com.offbynull.peernetic.rpc.invoke.InvokeThreadInformation;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * {@link UnstructuredService} implementation.
 * @author Kasra Faghihi
 * @param <A> address type
 */
final class UnstructuredServiceImplementation<A> implements UnstructuredService<A> {
    private LinkManager<A> linkManager;

    /**
     * Constructs an {@link UnstructuredServiceImplementation} object.
     * @param linkManager link manager
     * @throws NullPointerException if any arguments are {@code null}
     */
    public UnstructuredServiceImplementation(LinkManager<A> linkManager) {
        Validate.notNull(linkManager);
        this.linkManager = linkManager;
    }

    @Override
    public State<A> getState() {
        return linkManager.getState();
    }

    @Override
    public boolean join(byte[] secret) {
        Validate.isTrue(SECRET_SIZE == secret.length);
        
        A from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
        return linkManager.addIncomingLink(System.currentTimeMillis(), from, ByteBuffer.wrap(secret));
    }

    @Override
    public boolean keepAlive(byte[] secret) {
        Validate.isTrue(SECRET_SIZE == secret.length);
        
        A from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
        return linkManager.updateIncomingLink(System.currentTimeMillis(), from, ByteBuffer.wrap(secret));
    }
}
