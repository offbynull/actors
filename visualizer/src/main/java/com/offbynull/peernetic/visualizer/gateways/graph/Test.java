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

import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import java.util.Collections;

public final class Test {
    public static void main(String[] args) throws InterruptedException {
        GraphGateway graphGateway = new GraphGateway("graph");
        Shuttle shuttle = graphGateway.getIncomingShuttle();
        
        GraphGateway.startApplication();
        
        shuttle.send(Collections.singleton(new Message("test", "graph", new AddNode("node1", 0.0, 0.0, "-fx-background-color: linear-gradient(to bottom, #f2f2f2, #d4d4d4);"))));
        shuttle.send(Collections.singleton(new Message("test", "graph", new AddNode("node2", 300.0, 200.0, "-fx-background-color: blue"))));
        shuttle.send(Collections.singleton(new Message("test", "graph", new AddNode("node3", 200.0, 300.0, "-fx-background-color: red"))));
        shuttle.send(Collections.singleton(new Message("test", "graph", new AddNode("node4", 500.0, 50.0, "-fx-background-color: green"))));
        shuttle.send(Collections.singleton(new Message("test", "graph", new AddEdge("node1", "node2"))));
        shuttle.send(Collections.singleton(new Message("test", "graph", new AddEdge("node2", "node3"))));
        shuttle.send(Collections.singleton(new Message("test", "graph", new AddEdge("node3", "node4"))));
        shuttle.send(Collections.singleton(new Message("test", "graph", new AddEdge("node4", "node1"))));
        
        GraphGateway.awaitShutdown();
    }
}
