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
package com.offbynull.peernetic.demos.chord;

import com.offbynull.peernetic.actor.ActorRunner;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.network.IncomingMessageToEndpointAdapter;
import com.offbynull.peernetic.actor.network.NetworkEndpointFinder;
import com.offbynull.peernetic.nettyextensions.builders.NettyClient;
import com.offbynull.peernetic.nettyextensions.builders.NettyClientBuilder;
import com.offbynull.peernetic.nettyextensions.channels.simulatedpacket.RandomLine;
import com.offbynull.peernetic.nettyextensions.channels.simulatedpacket.TransitPacketRepository;
import com.offbynull.peernetic.nettyextensions.channels.simulatedpacket.WrappedSocketAddress;
import com.offbynull.peernetic.overlay.chord.ChordAccessor;
import com.offbynull.peernetic.overlay.chord.ChordConfig;
import com.offbynull.peernetic.overlay.chord.ChordOverlay;
import com.offbynull.peernetic.overlay.chord.ChordOverlayListener;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import com.offbynull.peernetic.overlay.common.visualizer.AddEdgeCommand;
import com.offbynull.peernetic.overlay.common.visualizer.AddNodeCommand;
import com.offbynull.peernetic.overlay.common.visualizer.ChangeNodeCommand;
import com.offbynull.peernetic.overlay.common.visualizer.Command;
import com.offbynull.peernetic.overlay.common.visualizer.JGraphXVisualizer;
import com.offbynull.peernetic.overlay.common.visualizer.RemoveEdgeCommand;
import com.offbynull.peernetic.overlay.common.visualizer.Visualizer;
import com.offbynull.peernetic.overlay.common.visualizer.VisualizerEventListener;
import com.offbynull.peernetic.overlay.common.visualizer.VisualizerUtils;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Range;

/**
 * Demo of {@link ChordOverlay}. Uses {@link TestTransport} as the underlying transport mechanism.
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
        
        
        
//        TransitPacketRepository hub = TransitPacketRepository.create(new PerfectLine());
        TransitPacketRepository hub = TransitPacketRepository.create(new RandomLine(12345L, // more realistic simulation
                Range.is(0.0001),
                Range.is(0.0001),
                Range.is(0.001),
                Range.is(0.1)));
        
        
        Endpoint overlay0Endpoint = null;
        int limit = 1023;
        BigInteger limigBigInt = new BigInteger("" + limit);
        
        for (int i = 0; i <= limit; i++) {
            if (i % 25 != 0) {
                continue;
            }
            
            final BigInteger iBigInt = new BigInteger("" + i);
            
            visualizer.step("Adding node " + i,
                    new AddNodeCommand<>(i),
                    new ChangeNodeCommand(i,
                            null,
                            VisualizerUtils.pointOnCircle(350, iBigInt.doubleValue() / (limigBigInt.doubleValue() + 1.0)),
                            null));
            
            ChordOverlayListener<SocketAddress> listener = new ChordOverlayListener<SocketAddress>() {
                
                private Integer oldPredecessor;
                private Set<Integer> oldFingers = Collections.emptySet();

                @Override
                public void stateUpdated(String event,
                        Pointer<SocketAddress> self,
                        Pointer<SocketAddress> predecessor,
                        List<Pointer<SocketAddress>> fingerTable,
                        List<Pointer<SocketAddress>> successorTable) {
                    
                    ArrayList<Command<Integer>> commands = new ArrayList<>();
                    
                    int selfId = self.getId().getValueAsBigInteger().intValue();
                    
                    // remove old pred and add new one in its place
                    if (oldPredecessor != null) {
                        commands.add(new RemoveEdgeCommand<>(selfId, oldPredecessor));
                    }
                    
                    if (predecessor != null) {
                        int newPredecessor = predecessor.getId().getValueAsBigInteger().intValue();
                        commands.add(new AddEdgeCommand<>(selfId, newPredecessor));
                        oldPredecessor = newPredecessor;
                    }
                    
                    // remove old fingers and add new ones in their place
                    for (int oldFinger : oldFingers) {
                        commands.add(new RemoveEdgeCommand<>(selfId, oldFinger));
                    }
                    
                    oldFingers = new HashSet<>();
                    for (Pointer<SocketAddress> newFingerPtr : fingerTable) {
                        int newFinger = newFingerPtr.getId().getValueAsBigInteger().intValue();
                        if (oldPredecessor == null || newFinger != oldPredecessor) { // don't add a new edge if an edge was already added
                                                                                     // for pred, two edges from same src/dst currently not
                                                                                     //supported
                            commands.add(new AddEdgeCommand<>(selfId, newFinger));
                            oldFingers.add(newFinger);
                        }
                    }
                    
                    // push changes
                    visualizer.step("Updated", commands.toArray(new Command[0]));
                }

                @Override
                public void failed(FailureMode failureMode) {
                    visualizer.step("Node Failed -- " + failureMode, new ChangeNodeCommand<>(iBigInt.intValue(), null, null, Color.RED));
                }
            };
            
            IncomingMessageToEndpointAdapter msgToEndpointAdapter = new IncomingMessageToEndpointAdapter();
            NettyClient client = new NettyClientBuilder()
                    .simulatedUdp(new WrappedSocketAddress(i), hub)
                    .addReadDestination(msgToEndpointAdapter)
                    .build();
            
            NetworkEndpointFinder finder = new NetworkEndpointFinder(client.channel());
            
            ChordConfig<SocketAddress> config = new ChordConfig<>(new WrappedSocketAddress(0), limigBigInt.toByteArray(), finder);
            config.setListener(listener);
            config.setBase(new Pointer<>(new Id(iBigInt.toByteArray(), limigBigInt.toByteArray()),
                    (SocketAddress) new WrappedSocketAddress(i)));
            if (i != 0) {
                config.setBootstrap(new Pointer<>(new Id(BigInteger.ZERO.toByteArray(), limigBigInt.toByteArray()),
                        (SocketAddress) new WrappedSocketAddress(0)));
            }
            
            ChordOverlay<SocketAddress> overlay = new ChordOverlay<>(config);
            ActorRunner overlayRunner = ActorRunner.createAndStart(overlay);
            if (i == 0) {
                overlay0Endpoint = overlayRunner.getEndpoint();
            }
            
            msgToEndpointAdapter.setEndpoint(overlayRunner.getEndpoint());
            
            Thread.sleep((long) (Math.random() * 1000L)); // sleep for 0 to 1 seconds, so things will look more varied when running
        }
        
        
        
        System.out.println("Type in ID to route...");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        while (true) {
            String idToFind = reader.readLine();
            ChordAccessor<Integer> accessor = new ChordAccessor(overlay0Endpoint);
            Pointer<Integer> res = accessor.routeTo(
                    new Id(new BigInteger(idToFind).toByteArray(), limigBigInt.toByteArray()),
                    5000L, TimeUnit.MILLISECONDS);
            
            System.out.println(res);
        }
    }
}
