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
package com.offbynull.peernetic.demos.unstructured;

import com.offbynull.peernetic.overlay.common.visualizer.AddEdgeCommand;
import com.offbynull.peernetic.overlay.common.visualizer.AddNodeCommand;
import com.offbynull.peernetic.overlay.common.visualizer.ChangeNodeCommand;
import com.offbynull.peernetic.overlay.common.visualizer.JGraphXVisualizer;
import com.offbynull.peernetic.overlay.common.visualizer.RemoveEdgeCommand;
import com.offbynull.peernetic.overlay.common.visualizer.Visualizer;
import com.offbynull.peernetic.overlay.common.visualizer.VisualizerEventListener;
import com.offbynull.peernetic.overlay.unstructured.LinkType;
import com.offbynull.peernetic.overlay.unstructured.UnstructuredOverlay;
import com.offbynull.peernetic.overlay.unstructured.UnstructuredOverlayConfig;
import com.offbynull.peernetic.overlay.unstructured.UnstructuredOverlayListener;
import com.offbynull.peernetic.rpc.FakeTransportFactory;
import com.offbynull.peernetic.rpc.Rpc;
import com.offbynull.peernetic.rpc.RpcConfig;
import com.offbynull.peernetic.rpc.common.filters.selfblock.SelfBlockId;
import com.offbynull.peernetic.rpc.common.filters.selfblock.SelfBlockIncomingFilter;
import com.offbynull.peernetic.rpc.common.filters.selfblock.SelfBlockOutgoingFilter;
import com.offbynull.peernetic.rpc.transports.fake.FakeHub;
import com.offbynull.peernetic.rpc.transports.fake.PerfectLine;
import java.awt.Color;
import java.awt.Point;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demo of {@link UnstructuredOverlay}. Uses {@link FakeTransport} as the underlying transport mechanism.
 * @author Kasra Faghihi
 */
public final class App {
    
    private App() {
        // do nothing
    }

    /**
     * Entry-point.
     * @param args unused
     * @throws Throwable on error
     */
    public static void main(String[] args) throws Throwable {
        final Visualizer<Integer> visualizer = new JGraphXVisualizer<>();
        visualizer.visualize(null, new VisualizerEventListener() {

            @Override
            public void closed() {
                System.exit(0);
            }
        });
        
        
        
        FakeHub<Integer> hub = new FakeHub<>(new PerfectLine<Integer>());
        hub.start();
        

        
        for (int i = 0; i < 3; i++) {
            visualizer.step("Adding node " + i,
                    new AddNodeCommand<>(i),
                    new ChangeNodeCommand(i, null, new Point((int) (Math.random() * 1000.0), (int) (Math.random() * 1000.0)),
                            Color.YELLOW));
            
            final int from = i;
            UnstructuredOverlayListener<Integer> listener = new UnstructuredOverlayListener<Integer>() {
                
                private AtomicInteger linkCount = new AtomicInteger();

                @Override
                public void linkCreated(UnstructuredOverlay<Integer> overlay, LinkType type, Integer address) {
                    if (type == LinkType.OUTGOING) {
                        int count = linkCount.incrementAndGet();
                        visualizer.step("Link created from " + from + " to " + address,
                                new AddEdgeCommand<>(from, address),
                                new ChangeNodeCommand<>(from, null, null, count == 0 ? Color.YELLOW : Color.GREEN));
                    }
                }

                @Override
                public void linkDestroyed(UnstructuredOverlay<Integer> overlay, LinkType type, Integer address) {
                    if (type == LinkType.OUTGOING) {
                        int count = linkCount.decrementAndGet();
                        visualizer.step("Link destroyed from " + from + " to " + address,
                                new RemoveEdgeCommand<>(from, address),
                                new ChangeNodeCommand<>(from, null, null, count == 0 ? Color.YELLOW : Color.GREEN));
                    }
                }

                @Override
                public void addressCacheEmpty(UnstructuredOverlay<Integer> overlay) {
                    overlay.addToAddressCache(0);
                }
            };
            
            RpcConfig<Integer> rpcConfig = new RpcConfig<>();
            SelfBlockId selfBlockId = new SelfBlockId();
            rpcConfig.setIncomingFilters(Arrays.asList(new SelfBlockIncomingFilter<Integer>(selfBlockId)));
            rpcConfig.setOutgoingFilters(Arrays.asList(new SelfBlockOutgoingFilter<Integer>(selfBlockId)));
            Rpc<Integer> rpc = new Rpc(new FakeTransportFactory(hub, i), rpcConfig);
            
            UnstructuredOverlayConfig<Integer> overlayConfig = new UnstructuredOverlayConfig<>();
            overlayConfig.setMaxOutgoingLinkAttemptsPerCycle(2);
            overlayConfig.setMaxOutgoingLinks(5);
            overlayConfig.setMaxIncomingLinks(5);
            overlayConfig.setCycleDuration(100L);
            overlayConfig.setOutgoingLinkStaleDuration(250L);
            overlayConfig.setOutgoingLinkExpireDuration(500L);
            overlayConfig.setIncomingLinkExpireDuration(500L);
            UnstructuredOverlay<Integer> overlay = new UnstructuredOverlay<>(rpc, listener, overlayConfig);
            overlay.startAndWait();
        }
    }
}
