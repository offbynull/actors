package com.offbynull.peernetic.examples.chord;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphNodeRemoveHandler;
import static com.offbynull.peernetic.visualizer.gateways.graph.GraphNodeRemoveHandler.RemoveMode.PERMANENT_TO_TEMPORARY;
import com.offbynull.peernetic.visualizer.gateways.graph.NodeProperties;
import org.apache.commons.lang3.Validate;

final class CustomGraphNodeRemoveHandler implements GraphNodeRemoveHandler {

    @Override
    public NodeProperties nodeRemoved(Address graphAddress, String id, RemoveMode removeMode, NodeProperties nodeProperties) {
        Validate.notNull(graphAddress);
        Validate.notNull(id);
        Validate.notNull(removeMode);
        Validate.notNull(nodeProperties);
        if (removeMode == PERMANENT_TO_TEMPORARY) {
            return new NodeProperties(nodeProperties.getText(), 0xFF0000, nodeProperties.getX(), nodeProperties.getY());
        } else {
            return nodeProperties;
        }
    }
    
}
