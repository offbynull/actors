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
import com.offbynull.peernetic.core.actor.ActorRunner;
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
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new LabelNode("n1", "new label for\n\nn1"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new MoveNode("n1", 0.0, 0.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new StyleNode("n1", 0xFF0000));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddNode("n2"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new MoveNode("n2", 200.0, 0.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new StyleNode("n2", 0x00FF00));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddNode("n3"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new MoveNode("n3", 0.0, 200.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new StyleNode("n3", 0x0000FF));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddEdge("n1", "n2"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddEdge("n2", "n3"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddEdge("n3", "n1"));

            // Create graph g2
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new AddNode("e1"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new MoveNode("e1", 0.0, 0.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new StyleNode("e1", 0xFF00FF));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new AddNode("e2"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new MoveNode("e2", 200.0, 200.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new StyleNode("e2", 0x00FFFF));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new AddEdge("e1", "e2"));
            
            // Create graph f2TempWith1InAnd1Out
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1InAnd1Out"), new AddNode("f1"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1InAnd1Out"), new MoveNode("f1", 0.0, 0.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1InAnd1Out"), new AddNode("f2"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1InAnd1Out"), new MoveNode("f2", 100.0, 0.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1InAnd1Out"), new AddNode("f3"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1InAnd1Out"), new MoveNode("f3", 0.0, 100.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1InAnd1Out"), new AddEdge("f1", "f2"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1InAnd1Out"), new AddEdge("f2", "f3"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1InAnd1Out"), new AddEdge("f1", "f3"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1InAnd1Out"), new RemoveNode("f2"));

            // Create graph f2TempWith1Out
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1Out"), new AddNode("f1"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1Out"), new MoveNode("f1", 0.0, 0.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1Out"), new AddNode("f2"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1Out"), new MoveNode("f2", 100.0, 0.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1Out"), new AddNode("f3"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1Out"), new MoveNode("f3", 0.0, 100.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1Out"), new AddEdge("f2", "f3"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1Out"), new AddEdge("f1", "f3"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1Out"), new RemoveNode("f2"));

            // Create graph f2TempWith1In
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1In"), new AddNode("f1"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1In"), new MoveNode("f1", 0.0, 0.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1In"), new AddNode("f2"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1In"), new MoveNode("f2", 100.0, 0.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1In"), new AddNode("f3"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1In"), new MoveNode("f3", 0.0, 100.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1In"), new AddEdge("f1", "f2"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1In"), new AddEdge("f1", "f3"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2TempWith1In"), new RemoveNode("f2"));

            // Create graph f2Gone
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2Gone"), new AddNode("f1"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2Gone"), new MoveNode("f1", 0.0, 0.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2Gone"), new AddNode("f2"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2Gone"), new MoveNode("f2", 100.0, 0.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2Gone"), new AddNode("f3"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2Gone"), new MoveNode("f3", 0.0, 100.0));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2Gone"), new AddEdge("f1", "f2"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2Gone"), new AddEdge("f1", "f3"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2Gone"), new RemoveNode("f2"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2Gone"), new RemoveEdge("f1", "f2"));
            ctx.addOutgoingMessage(graphPrefix.appendSuffix("f2Gone"), new RemoveEdge("f1", "f3"));
        };

        GraphGateway graphGateway = new GraphGateway("graph");
        Shuttle graphInputShuttle = graphGateway.getIncomingShuttle();
        GraphGateway.startApplication();

        ActorRunner testerRunner = new ActorRunner("local");

        testerRunner.addOutgoingShuttle(graphInputShuttle);
        testerRunner.addActor("tester", tester, Address.of("graph"));

        GraphGateway.awaitShutdown();
    }
}
