/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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

import com.offbynull.peernetic.actor.ActorRunner;
import com.offbynull.peernetic.actor.network.IncomingMessageToEndpointAdapter;
import com.offbynull.peernetic.actor.network.NetworkEndpointFinder;
import com.offbynull.peernetic.actor.network.NetworkEndpointKeyExtractor;
import com.offbynull.peernetic.networktools.netty.builders.NettyClient;
import com.offbynull.peernetic.networktools.netty.builders.NettyClientBuilder;
import com.offbynull.peernetic.networktools.netty.handlers.selfblock.SelfBlockId;
import com.offbynull.peernetic.networktools.netty.simulation.PerfectLine;
import com.offbynull.peernetic.networktools.netty.simulation.TransitPacketRepository;
import com.offbynull.peernetic.networktools.netty.simulation.WrappedSocketAddress;
import com.offbynull.peernetic.overlay.common.visualizer.AddEdgeCommand;
import com.offbynull.peernetic.overlay.common.visualizer.AddNodeCommand;
import com.offbynull.peernetic.overlay.common.visualizer.ChangeNodeCommand;
import com.offbynull.peernetic.overlay.common.visualizer.JGraphXVisualizer;
import com.offbynull.peernetic.overlay.common.visualizer.RemoveEdgeCommand;
import com.offbynull.peernetic.overlay.common.visualizer.Visualizer;
import com.offbynull.peernetic.overlay.common.visualizer.VisualizerEventListener;
import com.offbynull.peernetic.overlay.unstructured.LinkType;
import com.offbynull.peernetic.overlay.unstructured.UnstructuredOverlay;
import com.offbynull.peernetic.overlay.unstructured.UnstructuredOverlayListener;
import java.awt.Color;
import java.awt.Point;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Set;
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
        
        
        
        TransitPacketRepository hub = TransitPacketRepository.create(new PerfectLine());
//        TestHub<Integer> hub = new TestHub<>(new RandomLine<Integer>(12345L, // more realistic simulation
//                Range.is(0.0001),
//                Range.is(0.0),
//                Range.is(1.0),
//                Range.is(0.1)));
        
        
        for (int i = 0; i < 100; i++) {
            visualizer.step("Adding node " + i,
                    new AddNodeCommand<>(i),
                    new ChangeNodeCommand(i, null, new Point((int) (Math.random() * 1000.0), (int) (Math.random() * 1000.0)),
                            Color.YELLOW));
            
            final int from = i;
            UnstructuredOverlayListener<Integer> listener = new UnstructuredOverlayListener<Integer>() {

                private AtomicInteger linkCount = new AtomicInteger();

                @Override
                public void linkCreated(UnstructuredOverlay<Integer> overlay, LinkType type, Integer address) {
                    // THERE ARE THREAD SAFETY ISSUES HERE THAT WILL MANIFEST ON HIGH CHURN
                    if (type == LinkType.OUTGOING) {
                        int count = linkCount.incrementAndGet();
                        visualizer.step("Link created from " + from + " to " + address,
                                new AddEdgeCommand<>(from, address),
                                new ChangeNodeCommand<>(from, null, null, count == 0 ? Color.YELLOW : Color.GREEN));
                    }
                }

                @Override
                public void linkDestroyed(UnstructuredOverlay<Integer> overlay, LinkType type, Integer address) {
                    // THERE ARE THREAD SAFETY ISSUES HERE THAT WILL MANIFEST ON HIGH CHURN
                    if (type == LinkType.OUTGOING) {
                        int count = linkCount.decrementAndGet();
                        visualizer.step("Link destroyed from " + from + " to " + address,
                                new RemoveEdgeCommand<>(from, address),
                                new ChangeNodeCommand<>(from, null, null, count == 0 ? Color.YELLOW : Color.GREEN));
                    }
                }

                @Override
                public Set<Integer> addressCacheEmpty(UnstructuredOverlay overlay) {
                    return Collections.singleton(0);
                }
            };
            
            SelfBlockId selfBlockId = new SelfBlockId();
            IncomingMessageToEndpointAdapter msgToEndpointAdapter = new IncomingMessageToEndpointAdapter();
            NettyClient client = new NettyClientBuilder()
                    .simulatedUdp(new WrappedSocketAddress(i), hub)
                    .selfBlockId(selfBlockId)
                    .addReadDestination(msgToEndpointAdapter)
                    .build();
            
            NetworkEndpointFinder finder = new NetworkEndpointFinder(client.channel());
            NetworkEndpointKeyExtractor extractor = new NetworkEndpointKeyExtractor();
            
            UnstructuredOverlay<SocketAddress> overlay = new UnstructuredOverlay<>(listener, finder, extractor, 3, 10000L,
                    Collections.singleton((SocketAddress) new WrappedSocketAddress(0)));
            ActorRunner overlayRunner = ActorRunner.createAndStart(overlay);
            
            msgToEndpointAdapter.setEndpoint(overlayRunner.getEndpoint());
            
            Thread.sleep((long) (Math.random() * 1000L)); // sleep for 0 to 1 seconds, so things will look more varied when running
        }
    }
}
