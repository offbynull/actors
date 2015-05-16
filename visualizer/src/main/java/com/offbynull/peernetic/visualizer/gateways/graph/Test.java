/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.visualizer.gateways.graph;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Shuttle;

/**
 * Graph sanity test.
 *
 * @author Kasra Faghihi
 */
public final class Test {

    private Test() {
        // do nothing
    }
    
    /**
     * Main method.
     *
     * @param args unused
     * @throws InterruptedException if interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        Coroutine tester = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            Address graphPrefix = ctx.getIncomingMessage();
            
            // Create graph g1
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddNode("n1"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new MoveNode("n1", 0.0, 0.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new StyleNode("n1", "-fx-background-color: orange"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddNode("n2"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new MoveNode("n2", 200.0, 0.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new StyleNode("n2", "-fx-background-color: red"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddNode("n3"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new MoveNode("n3", 0.0, 200.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new StyleNode("n3", "-fx-background-color: green"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddEdge("n1", "n2"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddEdge("n2", "n3"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddEdge("n3", "n1"));

            // Create graph g2
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new AddNode("e1"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new MoveNode("e1", 0.0, 0.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new StyleNode("e1", "-fx-background-color: orange"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new AddNode("e2"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new MoveNode("e2", 200.0, 200.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new StyleNode("e2", "-fx-background-color: red"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new AddEdge("e1", "e2"));
        };

        GraphGateway graphGateway = new GraphGateway("graph");
        Shuttle graphInputShuttle = graphGateway.getIncomingShuttle();
        GraphGateway.startApplication();

        ActorThread testerThread = ActorThread.create("local");

        testerThread.addOutgoingShuttle(graphInputShuttle);
        testerThread.addCoroutineActor("tester", tester, Address.of("graph"));

        GraphGateway.awaitShutdown();
    }
}
