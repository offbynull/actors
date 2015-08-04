package com.offbynull.peernetic.examples.chord;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphNodeRemoveHandler;
import com.offbynull.peernetic.visualizer.gateways.graph.NodeProperties;

final class CustomGraphNodeRemoveHandler implements GraphNodeRemoveHandler {

    @Override
    public NodeProperties nodeRemoved(Address graphAddress, String id, RemoveMode removeMode, NodeProperties nodeProperties) {
        return new NodeProperties(nodeProperties.getText(), 0xFF0000, nodeProperties.getX(), nodeProperties.getY());
    }
    
}
