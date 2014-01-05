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
import com.offbynull.peernetic.common.utils.ByteBufferUtils;
import com.offbynull.peernetic.rpc.Rpc;
import com.offbynull.peernetic.rpc.invoke.AsyncResultListener;
import com.offbynull.peernetic.rpc.invoke.helpers.invocationchain.InvocationChainBuilder;
import com.offbynull.peernetic.rpc.invoke.helpers.invocationchain.InvocationChainStep;
import com.offbynull.peernetic.rpc.invoke.helpers.invocationchain.InvocationChainStepResultHandler;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

final class InternalUnstructuredOverlayUtils {
    private InternalUnstructuredOverlayUtils() {
    }
    
    static <A> void invokeJoin(final ActorQueueWriter selfWriter, final Rpc<A> rpc, final A address, final ByteBuffer secret) {
        Validate.notNull(address);
        Validate.notNull(secret);
        Validate.inclusiveBetween(UnstructuredService.SECRET_SIZE, UnstructuredService.SECRET_SIZE, secret.remaining());
        
        final UnstructuredServiceAsync service = rpc.accessService(address, UnstructuredService.SERVICE_ID, UnstructuredService.class,
                UnstructuredServiceAsync.class);
            
        InvocationChainBuilder builder = new InvocationChainBuilder();
        
        builder.addStep(new InvocationChainStep() {

            @Override
            public void doInvoke(AsyncResultListener resultListener) {
                service.getState(resultListener);
            }
        });
        
        builder.addStep(new InvocationChainStep() {

            @Override
            public void doInvoke(AsyncResultListener resultListener) {
                service.join(resultListener, ByteBufferUtils.copyContentsToArray(secret, false));
            }
        });
        
        builder.setResultHandler(new InvocationChainStepResultHandler() {

            @Override
            public boolean handleResult(InvocationChainStep step, int stepIndex, Object result) {
                if (stepIndex == 0) {
                    selfWriter.push(Message.createOneWayMessage(new AddToAddressCacheCommand<>(((State<A>) result).getIncomingLinks())));
                    selfWriter.push(Message.createOneWayMessage(new AddToAddressCacheCommand<>(((State<A>) result).getOutgoingLinks())));
                } else if (stepIndex == 1) {
                    Validate.notNull(result);

                    if ((Boolean) result) {
                        selfWriter.push(Message.createOneWayMessage(new JoinSuccessfulCommand<>(address, secret)));
                    } else {
                        selfWriter.push(Message.createOneWayMessage(new JoinFailedCommand<>(address, secret)));
                    }
                }
                return true;
            }
            
        });
        
        builder.build().start();
    }
    
    static <A> void invokeKeepAlive(final ActorQueueWriter selfWriter, final Rpc<A> rpc, final A address, final ByteBuffer secret) {
        Validate.notNull(address);
        Validate.notNull(secret);
        Validate.inclusiveBetween(UnstructuredService.SECRET_SIZE, UnstructuredService.SECRET_SIZE, secret.remaining());
        
        final UnstructuredServiceAsync service = rpc.accessService(address, UnstructuredService.SERVICE_ID, UnstructuredService.class,
                UnstructuredServiceAsync.class);
            
        InvocationChainBuilder builder = new InvocationChainBuilder();
        
        builder.addStep(new InvocationChainStep() {

            @Override
            public void doInvoke(AsyncResultListener resultListener) {
                service.getState(resultListener);
            }
        });
        
        builder.addStep(new InvocationChainStep() {

            @Override
            public void doInvoke(AsyncResultListener resultListener) {
                service.keepAlive(resultListener, ByteBufferUtils.copyContentsToArray(secret, false));
            }
        });
        
        builder.setResultHandler(new InvocationChainStepResultHandler() {

            @Override
            public boolean handleResult(InvocationChainStep step, int stepIndex, Object result) {
                if (stepIndex == 0) {
                    selfWriter.push(Message.createOneWayMessage(new AddToAddressCacheCommand<>(((State<A>) result).getIncomingLinks())));
                    selfWriter.push(Message.createOneWayMessage(new AddToAddressCacheCommand<>(((State<A>) result).getOutgoingLinks())));
                } else if (stepIndex == 1) {
                    Validate.notNull(result);

                    if ((Boolean) result) {
                        selfWriter.push(Message.createOneWayMessage(new KeepAliveSuccessfulCommand<>(address, secret)));
                    } else {
                        selfWriter.push(Message.createOneWayMessage(new KeepAliveFailedCommand<>(address, secret)));
                    }
                }
                return true;
            }
            
        });
        
        builder.build().start();
    }
}
