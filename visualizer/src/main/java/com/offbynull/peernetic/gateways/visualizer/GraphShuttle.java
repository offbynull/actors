package com.offbynull.peernetic.gateways.visualizer;

import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.common.AddressUtils;
import java.util.Collection;
import org.apache.commons.lang3.Validate;

final class GraphShuttle implements Shuttle {

    private final String prefix;

    public GraphShuttle(String prefix) {
        Validate.notNull(prefix);
        this.prefix = prefix;
    }
    
    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        
        GraphApplication graph = GraphApplication.getInstance();
        if (graph == null) {
            // TODO log warning here
            return;
        }
        
        messages.forEach(x -> {
            String dst = x.getDestinationAddress();
            String dstPrefix = AddressUtils.getAddressElement(dst, 0);
            Validate.isTrue(dstPrefix.equals(prefix));

            Object msg = x.getMessage();
            
            if (msg instanceof AddNode) {
                AddNode addNode = (AddNode) msg;
                graph.addNode(
                        addNode.getId(),
                        addNode.getX(),
                        addNode.getY(),
                        addNode.getStyle());
            } else if (msg instanceof MoveNode) {
                MoveNode moveNode = (MoveNode) msg;
                graph.moveNode(
                        moveNode.getId(),
                        moveNode.getX(),
                        moveNode.getY());
            } else if (msg instanceof StyleNode) {
                StyleNode styleNode = (StyleNode) msg;
                graph.styleNode(
                        styleNode.getId(),
                        styleNode.getStyle());
            } else if (msg instanceof RemoveNode) {
                RemoveNode removeNode = (RemoveNode) msg;
                graph.removeNode(
                        removeNode.getId());
            } else if (msg instanceof AddEdge) {
                AddEdge addEdge = (AddEdge) msg;
                graph.addEdge(
                        addEdge.getFromId(),
                        addEdge.getToId());
            } else if (msg instanceof StyleEdge) {
                StyleEdge styleEdge = (StyleEdge) msg;
                graph.styleEdge(
                        styleEdge.getFromId(),
                        styleEdge.getToId(),
                        styleEdge.getStyle());
            } else if (msg instanceof RemoveEdge) {
                RemoveEdge removeEdge = (RemoveEdge) msg;
                graph.removeEdge(
                        removeEdge.getFromId(),
                        removeEdge.getToId());
            } else {
                // TODO log here
            }
        });
    }
    
}
