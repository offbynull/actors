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
package com.offbynull.peernetic.demo.unstructured;

import com.offbynull.peernetic.overlay.common.visualizer.AddEdgeCommand;
import com.offbynull.peernetic.overlay.common.visualizer.AddNodeCommand;
import com.offbynull.peernetic.overlay.common.visualizer.ChangeNodeCommand;
import com.offbynull.peernetic.overlay.common.visualizer.JGraphXVisualizer;
import com.offbynull.peernetic.overlay.common.visualizer.RemoveEdgeCommand;
import com.offbynull.peernetic.overlay.common.visualizer.Visualizer;
import com.offbynull.peernetic.overlay.common.visualizer.VisualizerEventListener;
import com.offbynull.peernetic.overlay.unstructured.LinkManager;
import com.offbynull.peernetic.overlay.unstructured.LinkManagerListener;
import com.offbynull.peernetic.overlay.unstructured.LinkType;
import com.offbynull.peernetic.overlay.unstructured.UnstructuredOverlay;
import com.offbynull.peernetic.overlay.unstructured.UnstructuredService;
import com.offbynull.peernetic.overlay.unstructured.UnstructuredServiceImplementation;
import com.offbynull.peernetic.rpc.FakeTransportFactory;
import com.offbynull.peernetic.rpc.Rpc;
import com.offbynull.peernetic.rpc.RpcConfig;
import com.offbynull.peernetic.rpc.common.filters.selfblock.SelfBlockId;
import com.offbynull.peernetic.rpc.common.filters.selfblock.SelfBlockIncomingFilter;
import com.offbynull.peernetic.rpc.common.filters.selfblock.SelfBlockOutgoingFilter;
import com.offbynull.peernetic.rpc.transport.fake.FakeHub;
import com.offbynull.peernetic.rpc.transport.fake.PerfectLine;
import java.awt.Color;
import java.awt.Point;
import java.util.Arrays;
import java.util.Random;

/**
 * Demo of unstructured overlay.
 * @author Kasra Faghihi
 */
public final class App {
    
    private App() {
        // do nothing
    }

    /**
     * Main method.
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
        

        
        for (int i = 0; i < 100; i++) {
            visualizer.step("Adding node " + i,
                    new AddNodeCommand<>(i),
                    new ChangeNodeCommand(i, null, new Point((int) (Math.random() * 1000.0), (int) (Math.random() * 1000.0)),
                            Color.YELLOW));
            
            final int from = i;
            LinkManagerListener<Integer> listener = new LinkManagerListener<Integer>() {
                
                private volatile int linkCount;

                @Override
                public void linkCreated(LinkManager<Integer> linkManager, LinkType type, Integer address) {
                    if (type == LinkType.OUTGOING) {
                        linkCount++;
                        visualizer.step("Link created from " + from + " to " + address,
                                new AddEdgeCommand<>(from, address),
                                new ChangeNodeCommand<>(from, null, null, linkCount == 0 ? Color.YELLOW : Color.GREEN));
                    }
                }

                @Override
                public void linkDestroyed(LinkManager<Integer> linkManager, LinkType type, Integer address) {
                    if (type == LinkType.OUTGOING) {
                        linkCount--;
                        visualizer.step("Link destroyed from " + from + " to " + address,
                                new RemoveEdgeCommand<>(from, address),
                                new ChangeNodeCommand<>(from, null, null, linkCount == 0 ? Color.YELLOW : Color.GREEN));
                    }
                }

                @Override
                public void addressCacheEmpty(LinkManager<Integer> linkManager) {
                    linkManager.addToAddressCache(0);
                }
            };
            
            RpcConfig<Integer> rpcConfig = new RpcConfig<>();
            SelfBlockId selfBlockId = new SelfBlockId();
            rpcConfig.setIncomingFilters(Arrays.asList(new SelfBlockIncomingFilter<Integer>(selfBlockId)));
            rpcConfig.setOutgoingFilters(Arrays.asList(new SelfBlockOutgoingFilter<Integer>(selfBlockId)));
            Rpc<Integer> rpc = new Rpc(new FakeTransportFactory(hub, i), rpcConfig);
            
            LinkManager linkManager = new LinkManager(rpc, new Random(), listener, 5, 5, 5000L, 2500L, 5000L);
            
            rpc.addService(UnstructuredService.SERVICE_ID, new UnstructuredServiceImplementation<>(linkManager));
            
            UnstructuredOverlay<Integer> overlay = new UnstructuredOverlay<>(linkManager);
            overlay.startAndWait();
        }
    }
}
