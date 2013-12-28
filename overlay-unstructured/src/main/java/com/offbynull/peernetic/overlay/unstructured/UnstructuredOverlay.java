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

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.offbynull.peernetic.rpc.Rpc;
import org.apache.commons.lang3.Validate;

/**
 * An unstructured overlay service.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class UnstructuredOverlay<A> extends AbstractExecutionThreadService {
    private Rpc<A> rpc;
    private LinkManager<A> linkManager;
    private UnstructuredServiceImplementation<A> unstructuredService;
    private Thread thread;

    /**
     * Constructs a {@link UnstructuredOverlay} object with default configurations. Equivalent to calling
     * {@code UnstructuredOverlay(rpc, listener, new UnstructuredOverlayConfig())}.
     * @param rpc RPC
     * @param listener listener
     * @throws NullPointerException if any arguments are {@code null}
     */
    public UnstructuredOverlay(Rpc<A> rpc, UnstructuredOverlayListener<A> listener) {
        this(rpc, listener, new UnstructuredOverlayConfig<A>());
    }

    /**
     * Constructs a {@link UnstructuredOverlay} object.
     * @param rpc RPC
     * @param listener listener
     * @param config configs
     * @throws NullPointerException if any arguments other than {@code listener} are {@code null}
     */
    public UnstructuredOverlay(Rpc<A> rpc, UnstructuredOverlayListener<A> listener, UnstructuredOverlayConfig<A> config) {
        Validate.notNull(rpc);

        final UnstructuredOverlayListener<A> unstructuredOverlayListener = listener;
        LinkManagerListener<A> linkManagerListener = new LinkManagerListener<A>() {

            @Override
            public void linkCreated(LinkManager<A> linkManager, LinkType type, A address) {
                if (unstructuredOverlayListener != null) {
                    unstructuredOverlayListener.linkCreated(UnstructuredOverlay.this, type, address);
                }
            }

            @Override
            public void linkDestroyed(LinkManager<A> linkManager, LinkType type, A address) {
                if (unstructuredOverlayListener != null) {
                    unstructuredOverlayListener.linkDestroyed(UnstructuredOverlay.this, type, address);
                }
            }

            @Override
            public void addressCacheEmpty(LinkManager<A> linkManager) {
                if (unstructuredOverlayListener != null) {
                    unstructuredOverlayListener.addressCacheEmpty(UnstructuredOverlay.this);
                }
            }
        };
        
        this.linkManager = new LinkManager<>(rpc,
                config.getRandom(),
                linkManagerListener,
                config.getMaxIncomingLinks(),
                config.getMaxOutgoingLinks(),
                config.getIncomingLinkExpireDuration(),
                config.getOutgoingLinkStaleDuration(),
                config.getOutgoingLinkExpireDuration(),
                config.getCycleDuration(),
                config.getMaxOutgoingLinkAttemptsPerCycle());

        this.unstructuredService = new UnstructuredServiceImplementation(linkManager);
        this.rpc = rpc;
    }

    @Override
    protected void startUp() throws Exception {
        thread = Thread.currentThread();
        rpc.addService(UnstructuredServiceImplementation.SERVICE_ID, unstructuredService);
    }

    @Override
    protected void run() throws Exception {
        while (true) {
            long timestamp = System.currentTimeMillis();
            long nextTimestamp = linkManager.process(timestamp);
            
            Thread.sleep(Math.max(1L, nextTimestamp - timestamp));
        }
    }

    @Override
    protected void shutDown() throws Exception {
        rpc.removeService(UnstructuredServiceImplementation.SERVICE_ID);
    }

    @Override
    protected void triggerShutdown() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * Add addresses to address cache.
     * <p/>
     * Addresses used for creating outgoing links are obtained through the address cache. The address cache automatically gets populated as
     * this node traverses over the other nodes in the overlay. When the address cache is empty,
     * {@link UnstructuredOverlayListener#addressCacheEmpty(com.offbynull.peernetic.overlay.unstructured.UnstructuredOverlay) } gets called.
     * @param addresses addresses to add
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void addToAddressCache(A... addresses) {
        linkManager.addToAddressCache(addresses);
    }

}
