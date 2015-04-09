package com.offbynull.peernetic.visualization;

import com.offbynull.peernetic.core.Message;
import com.offbynull.peernetic.core.Shuttle;
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
        
        Thread.sleep(100000L);
    }
}
